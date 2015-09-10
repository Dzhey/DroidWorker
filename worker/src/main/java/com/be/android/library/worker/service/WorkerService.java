package com.be.android.library.worker.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.base.JobStatus;
import com.be.android.library.worker.controllers.JobManager;
import com.be.android.library.worker.interfaces.Job;
import com.be.android.library.worker.interfaces.JobEventListener;
import com.be.android.library.worker.interfaces.Worker;

import java.util.HashSet;
import java.util.Set;

public abstract class WorkerService extends Service {

    public static final String LOG_TAG = WorkerService.class.getSimpleName();
    public static final String EXTRA_JOB_ID = "job_id";

    public static final String ACTION_SUBMIT_JOB = "com.be.android.library" +
                                                   ".worker.intent.action.SUBMIT_JOB";

    private static final int STOP_SELF_DELAY_MILLIS_DEFAULT = 18000;

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
            if (!event.isJobFinished()) {
                return;
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    WorkerService.this.onJobFinishedInner(event);
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

        Job job = getJobManager().findJob(jobId);
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
        registerJob(job);

        if (mWorker == null) {
            synchronized (this) {
                if (mWorker == null) {
                    mWorker = createWorker(mLaunchIntent);
                }
            }
        }

        try {
            mWorker.submitJob(job);
            onJobSubmitted(job);

        } catch (Exception e) {
            Log.e(LOG_TAG, String.format("unable to submit job '%s'", job));
            e.printStackTrace();
        }
    }

    private void registerJob(Job job) {
        if (!isRunning) {
            throw new IllegalStateException("service is already stopped");
        }

        lastJobSubmitTimeMillis = System.currentTimeMillis();

        mPendingJobs.add(job.getJobId());
        job.addJobEventListener(mJobFinishedListener);
    }

    protected JobManager getJobManager() {
        return JobManager.getInstance();
    }

    protected abstract Worker createWorker(Intent launchIntent);

    public boolean hasPendingJobs() {
        return !mPendingJobs.isEmpty();
    }

    public Set<Integer> getPendingJobs() {
        return new HashSet<>(mPendingJobs);
    }

    protected int getKeepAliveDurationMillis() {
        return STOP_SELF_DELAY_MILLIS_DEFAULT;
    }

    protected void onJobSubmitted(Job job) {

    }

    protected void onJobFinished(JobEvent result) {
    }

    private void onJobFinishedInner(JobEvent result) {
        if (result.getJobId() == JobManager.JOB_ID_UNSPECIFIED) {
            Log.e(LOG_TAG, "finished job has unspecified job id!");
            return;
        }

        mPendingJobs.remove(result.getJobId());
        scheduleStopSelf();

        onJobFinished(result);
    }

    private void scheduleStopSelf() {
        if (!hasPendingJobs()) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopSelfIfNoJobs();
                }
            }, getKeepAliveDurationMillis());
        }
    }

    protected void stopSelfIfNoJobs() {
        if (!hasPendingJobs()) {
            final long deltaMillis = System.currentTimeMillis() - lastJobSubmitTimeMillis;
            final int delay = getKeepAliveDurationMillis();
            final long precisionFix = delay / 20;
            if (deltaMillis < delay - precisionFix) {
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
