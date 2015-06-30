package com.be.android.library.worker.demo.jobs;

import com.be.android.library.worker.jobs.LoadJob;
import com.be.android.library.worker.models.LoadJobResult;

import java.util.concurrent.CountDownLatch;

public class CancelableJob extends LoadJob {

    private final CountDownLatch mLatch;

    public CancelableJob() {
        mLatch = new CountDownLatch(1);
    }

    @Override
    protected LoadJobResult<?> performLoad() throws Exception {
        mLatch.await();

        return LoadJobResult.loadOk();
    }

    @Override
    protected void onCancelled() {
        mLatch.countDown();
    }
}
