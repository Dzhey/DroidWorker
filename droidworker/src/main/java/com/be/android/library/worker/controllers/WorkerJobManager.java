package com.be.android.library.worker.controllers;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.be.android.library.worker.interfaces.Job;
import com.be.android.library.worker.service.ThreadPoolWorkerService;
import com.be.android.library.worker.service.WorkerService;

public class WorkerJobManager extends JobManager {

    private static final String LOG_TAG = WorkerJobManager.class.getSimpleName();

    private final int mWorkerThreadPoolSize;
    private final Context mContext;
    private final Class<?> mWorkerServiceClass;

    public WorkerJobManager(Context context) {
        this(context, ThreadPoolWorkerService.THREAD_POOL_SIZE_DEFAULT, ThreadPoolWorkerService.class);
    }

    public WorkerJobManager(Context context, int workerThreadPoolSize, Class<?> workerServiceClass) {
        if (workerThreadPoolSize < 1) {
            throw new IllegalArgumentException("mWorkerThreadPoolSize < 1");
        }

        if (!WorkerService.class.isAssignableFrom(workerServiceClass)) {
            throw new IllegalArgumentException(String.format(
                    "specified class '%s' is not assignable from '%s'",
                    workerServiceClass.getName(), WorkerService.class.getName()));
        }

        mContext = context;
        mWorkerThreadPoolSize = workerThreadPoolSize;
        mWorkerServiceClass = workerServiceClass;
    }

    @Override
    protected void submitJobImpl(Job job) {
        WorkerService service = WorkerService.getInstance();
        if (service != null && service.isRunning()) {
            service.submitJob(job);

            return;
        }

        Intent submitIntent = createSubmitIntent(mContext, job);
        try {
            if (mContext.startService(submitIntent) == null) {
                Log.e(LOG_TAG, "unable to start worker service");

                throw new RuntimeException("in case to submit job to " +
                        "worker service one should register it first in AndroidManifset.xml");
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "unable to start worker service");
            e.printStackTrace();
            throw new RuntimeException("unable to start worker service", e);
        }
    }

    private Intent createSubmitIntent(Context context, Job job) {
        Intent intent = new Intent(WorkerService.ACTION_SUBMIT_JOB);
        intent.setClass(context.getApplicationContext(), mWorkerServiceClass);
        intent.putExtra(WorkerService.EXTRA_JOB_ID, job.getJobId());
        intent.putExtra(ThreadPoolWorkerService.EXTRA_THREAD_POOL_SIZE, mWorkerThreadPoolSize);

        addSubmitIntentParams(intent, job);

        return intent;
    }

    protected void addSubmitIntentParams(Intent submitIntent, Job job) {
    }
}
