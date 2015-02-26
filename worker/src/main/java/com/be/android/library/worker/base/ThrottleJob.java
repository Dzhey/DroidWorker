package com.be.android.library.worker.base;

import com.be.android.library.worker.exceptions.JobExecutionException;

import java.util.concurrent.atomic.AtomicBoolean;

public class ThrottleJob extends ForkJoinJob {

    public class Throttle {
        private final Object mutex = new Object();
        private int mTimeout;
        private AtomicBoolean mIsRefreshed = new AtomicBoolean(true);

        protected void waitForTimeout() throws InterruptedException {
            synchronized (mutex) {
                while (mIsRefreshed.getAndSet(false)) {
                    mutex.wait(mTimeout);
                }
            }
        }

        public void updateTimeout(int timeoutMillis) {
            synchronized (mutex) {
                mTimeout = timeoutMillis;
                mIsRefreshed.set(true);
                mutex.notify();
            }
        }
    }

    private ForkJoinJob mTargetJob;
    private Throttle mThrottle;

    public ThrottleJob() {
        mThrottle = new Throttle();
    }

    @Override
    protected final JobEvent executeImpl() throws Exception {
        final ForkJoinJob targetJob = mTargetJob;

        if (targetJob == null) {
            throw new IllegalStateException("target job is not defined; call setTargetJob()");
        }

        mThrottle.waitForTimeout();

        return JobEvent.fromEvent(executeTargetJob(targetJob));
    }

    protected JobEvent executeTargetJob(ForkJoinJob mTargetJob) throws JobExecutionException {
        return buildFork(mTargetJob)
                .groupOnTheSameGroup()
                .setForwardEvents(true)
                .fork()
                .join();
    }

    public ForkJoinJob getTargetJob() {
        return mTargetJob;
    }

    public void setTargetJob(ForkJoinJob targetJob) {
        mTargetJob = targetJob;
    }

    public Throttle getThrottle() {
        return mThrottle;
    }
}
