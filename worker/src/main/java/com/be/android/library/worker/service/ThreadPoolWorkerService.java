package com.be.android.library.worker.service;

import android.content.Intent;

import com.be.android.library.worker.controllers.ThreadPoolWorker;

public class ThreadPoolWorkerService extends WorkerService {

    public static final String EXTRA_THREAD_POOL_SIZE = "thread_pool_size";
    public static final String EXTRA_ENABLE_LOG_TRACE = "enable_log_trace";

    public static final int THREAD_POOL_SIZE_DEFAULT = 4;

    private ThreadPoolWorker mWorker;

    @Override
    protected ThreadPoolWorker createWorker(Intent launchIntent) {
        int threadCount = THREAD_POOL_SIZE_DEFAULT;
        boolean enableLogTrace = false;

        if (launchIntent != null) {
            threadCount = launchIntent.getIntExtra(EXTRA_THREAD_POOL_SIZE, THREAD_POOL_SIZE_DEFAULT);
            enableLogTrace = launchIntent.getBooleanExtra(EXTRA_ENABLE_LOG_TRACE, false);
        }

        mWorker = new ThreadPoolWorker(threadCount);
        mWorker.setTraceEnabled(enableLogTrace);

        return mWorker;
    }

    @Override
    public void onDestroy() {
        mWorker = null;

        super.onDestroy();
    }
}
