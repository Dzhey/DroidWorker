package com.be.android.library.worker.base;

import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class JobStatusHolder {

    private JobStatus mJobStatus = JobStatus.PENDING;
    private LinkedList<JobStatusLock> mLocks;
    private Lock mLock;

    public JobStatusHolder() {
        mLocks = new LinkedList<JobStatusLock>();
        mLock = new ReentrantLock(false);
    }

    public JobStatus getJobStatus() {
        return mJobStatus;
    }

    public void setJobStatus(JobStatus jobStatus) throws InterruptedException {
        awaitStatusLocks();

        mLock.lock();
        try {
            mJobStatus = jobStatus;

        } finally {
            mLock.unlock();
        }
    }

    public JobStatusLock newLock() {
        mLock.lock();
        try {
            JobStatusLock lock = createLock();
            mLocks.add(lock);

            return lock;

        } finally {
            mLock.unlock();
        }
    }

    protected JobStatusLock createLock() {
        return new JobStatusLock();
    }

    private void awaitStatusLocks() throws InterruptedException {
        while (true) {
            mLock.lock();
            JobStatusLock lock = null;
            try {
                if (mLocks.isEmpty()) {
                    break;
                }
                lock = mLocks.poll();

            } finally {
                mLock.unlock();
            }

            lock.await();
        }
    }
}
