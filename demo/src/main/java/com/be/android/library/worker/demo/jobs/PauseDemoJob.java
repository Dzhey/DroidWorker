package com.be.android.library.worker.demo.jobs;

import android.os.SystemClock;
import android.util.Log;

import com.be.android.library.worker.base.BaseJob;
import com.be.android.library.worker.base.JobEvent;
import com.be.library.worker.annotations.Inherited;

@Inherited(SimpleImageLoaderJob.class)
public class PauseDemoJob extends BaseJob {

    private static final String LOG_TAG = PauseDemoJob.class.getSimpleName();

    @Inherited
    String mImageUrl;

    @Override
    protected JobEvent executeImpl() throws Exception {
        float progress = 0f;

        while (progress < 1f) {
            if (isCancelled()) {
                Log.i("PauseDemoJob", "job cancelled");
                return JobEvent.ok();
            }

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
        final long start = SystemClock.elapsedRealtime();
        super.onPerformPause();
        final long duration = SystemClock.elapsedRealtime() - start;
        Log.i(LOG_TAG, "onPerformPause() resumed; pause duration: " + duration + "ms");
    }
}
