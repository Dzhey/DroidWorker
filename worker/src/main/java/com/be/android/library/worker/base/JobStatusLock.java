package com.be.android.library.worker.base;

import java.util.concurrent.CountDownLatch;

public class JobStatusLock {

    private final CountDownLatch mLatch;

    public JobStatusLock() {
        mLatch = new CountDownLatch(1);
    }

    public void lock() throws InterruptedException {
        mLatch.await();
    }

    public void release() {
        mLatch.countDown();
    }
}
