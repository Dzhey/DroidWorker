package com.be.android.library.worker.base;

import java.util.concurrent.CountDownLatch;

public class JobStatusLock {

    private final CountDownLatch mLatch;

    JobStatusLock() {
        mLatch = new CountDownLatch(1);
    }

    CountDownLatch getLatch() {
        return mLatch;
    }

    public void release() {
        mLatch.countDown();
    }
}
