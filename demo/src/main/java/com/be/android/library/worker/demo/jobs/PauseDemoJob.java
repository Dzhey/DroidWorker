package com.be.android.library.worker.demo.jobs;

import android.util.Log;

import com.be.android.library.worker.base.BaseJob;
import com.be.android.library.worker.base.JobEvent;

public class PauseDemoJob extends BaseJob {

    private static final String LOG_TAG = PauseDemoJob.class.getSimpleName();

    @Override
    protected JobEvent executeImpl() throws Exception {
        float progress = 0f;

        while (progress < 1f) {
            yieldForPause();
            Thread.sleep(60);

            progress += 0.01f;

            notifyProgressUpdate(progress);
        }

        return JobEvent.ok();
    }

    @Override
    protected void onPerformPause() throws InterruptedException {
        Log.i(LOG_TAG, "onPerformPause() called");
        super.onPerformPause();
        Log.i(LOG_TAG, "onPerformPause() resumed");
    }
}
