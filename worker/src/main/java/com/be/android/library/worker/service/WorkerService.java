package com.be.android.library.worker.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.be.android.library.worker.base.JobStatus;
import com.be.android.library.worker.controllers.JobManager;
import com.be.android.library.worker.interfaces.JobEventListener;
import com.be.android.library.worker.interfaces.Worker;
import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.interfaces.Job;

import java.util.HashSet;
import java.util.Set;

public abstract class WorkerService extends Service {

    public static final String LOG_TAG = WorkerService.class.getSimpleName();
    public static final String EXTRA_JOB_ID = "job_id";

    public static final String ACTION_SUBMIT_JOB = "com.be.android.library" +
                                                   ".worker.intent.action.SUBMIT_JOB";

    private static final int STOP_SELF_DELAY_MILLIS = 18000;

    private static WorkerService mInstance;

    private Handler mHandler;
    private Worker mWorker;
    private Set<Integer> mPendingJobs;
    private boolean isRunning;
    private long lastJobSubmitTimeMillis;
    private Intent mLaunchIntent;

    private final JobEventListener mJobFinishedListener = new JobEventListener() {
        @Override
        public void onJobEvent(final JobEvent event) {
            if (event.isJobFinished() == false) return;

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    WorkerService.this.onJobFinished(event);
                }
            });
        }
    };

    public static WorkerService getInstance() {
        return mInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mPendingJobs = new HashSet<Integer>();
        mHandler = new Handler();
        isRunning = true;

        Log.d(LOG_TAG, "WorkerService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ACTION_SUBMIT_JOB.equals(intent.getAction()) == false) {
            Log.e(LOG_TAG, String.format("unsupported intent action received: %s", intent.getAction()));
            stopSelf(startId);

            return Service.START_NOT_STICKY;
        }

        int jobId = intent.getIntExtra(EXTRA_JOB_ID, JobManager.JOB_ID_UNSPECIFIED);
        if (jobId == JobManager.JOB_ID_UNSPECIFIED) {
            Log.e(LOG_TAG, "job id is not specified");
            stopSelf(startId);

            return Service.START_NOT_STICKY;
        }

        Job job = JobManager.getInstance().findJob(jobId);
        if (job == null) {
            Log.e(LOG_TAG, String.format("job id '%d' not found", jobId));
            stopSelf(startId);

            return Service.START_NOT_STICKY;
        }
        if (job.getStatus() != JobStatus.PENDING) {
            Log.w(LOG_TAG, String.format("job '%s' is already submitted", job));
            stopSelf(startId);

            return Service.START_NOT_STICKY;
        }

        mLaunchIntent = intent;
        submitJob(job);

        return Service.START_REDELIVER_INTENT;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void submitJob(Job job) {
        if (isRunning == false) {
            throw new IllegalStateException("service is already stopped");
        }

        if (mWorker == null) {
            synchronized (this) {
                if (mWorker == null) {
                    mWorker = createWorker(mLaunchIntent);
                }
            }
        }

        lastJobSubmitTimeMillis = System.currentTimeMillis();

        mPendingJobs.add(job.getJobId());
        job.addJobEventListener(mJobFinishedListener);

        try {
            mWorker.submitJob(job);

        } catch (Exception e) {
            Log.e(LOG_TAG, String.format("unable to submit job '%s'", job));
            e.printStackTrace();
        }
    }

    protected abstract Worker createWorker(Intent launchIntent);

    private void onJobFinished(JobEvent result) {
        if (result.getJobId() == JobManager.JOB_ID_UNSPECIFIED) {
            Log.e(LOG_TAG, "finished job has unspecified job id!");
            return;
        }

        mPendingJobs.remove(result.getJobId());
        scheduleStopSelf();
    }

    private void scheduleStopSelf() {
        if (mPendingJobs.isEmpty()) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopSelfIfNoJobs();
                }
            }, STOP_SELF_DELAY_MILLIS);
        }
    }

    private void stopSelfIfNoJobs() {
        if (mPendingJobs.isEmpty()) {
            long deltaMillis = System.currentTimeMillis() - lastJobSubmitTimeMillis;
            long precisionFix = STOP_SELF_DELAY_MILLIS / 20;
            if (deltaMillis < STOP_SELF_DELAY_MILLIS - precisionFix) {
                return;
            }
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        isRunning = false;

        if (mWorker != null) {
            synchronized (this) {
                mWorker.finish();
                mWorker = null;
            }
        }

        mInstance = null;

        Log.d(LOG_TAG, "WorkerService destroyed");

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
