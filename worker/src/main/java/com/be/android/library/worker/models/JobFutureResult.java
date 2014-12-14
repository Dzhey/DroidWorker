package com.be.android.library.worker.models;

import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.base.JobStatus;
import com.be.android.library.worker.controllers.JobManager;
import com.be.android.library.worker.interfaces.Job;
import com.be.android.library.worker.interfaces.JobEventListener;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class JobFutureResult implements Future<JobEvent> {

    private volatile boolean mIsCancelled;
    private volatile int mJobId = JobManager.JOB_ID_UNSPECIFIED;
    private JobEvent jobEvent;
    private final Object mMutex = new Object();
    private final CountDownLatch mWaitLatch = new CountDownLatch(1);

    private final JobEventListener mJobEventListener =
            new JobEventListener() {
                @Override
                public void onJobEvent(JobEvent event) {
                    if (mJobId == JobManager.JOB_ID_UNSPECIFIED
                            && event.getEventCode() == JobEvent.UPDATE_CODE_STATUS_CHANGED) {

                        mJobId = event.getJobId();

                        if (mIsCancelled) {
                            JobManager.getInstance().cancelJob(mJobId);
                            mWaitLatch.countDown();
                            return;
                        }
                    }

                    if (event.isJobFinished() == false) return;

                    synchronized (mMutex) {
                        jobEvent = event;
                    }
                    mWaitLatch.countDown();
                }
            };

    public JobFutureResult(Job job) {
        if (job.getStatus() != JobStatus.PENDING) {
            throw new IllegalStateException(String.format("Can't create pending " +
                    "future result for submitted job; \"%s\"", job));
        }

        job.addJobEventListener(mJobEventListener);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (mIsCancelled) return true;

        mIsCancelled = true;

        if (mJobId != JobManager.JOB_ID_UNSPECIFIED) {
            mWaitLatch.countDown();

            return JobManager.getInstance().cancelJob(mJobId);
        }

        return true;
    }

    @Override
    public boolean isCancelled() {
        return mIsCancelled;
    }

    @Override
    public boolean isDone() {
        synchronized (mMutex) {
            return jobEvent != null;
        }
    }

    @Override
    public JobEvent get() throws InterruptedException, ExecutionException {
        synchronized (mMutex) {
            if (jobEvent != null) {
                return jobEvent;
            }
        }

        mWaitLatch.await();

        return jobEvent;
    }

    @Override
    public JobEvent get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {

        synchronized (mMutex) {
            if (jobEvent != null) {
                return jobEvent;
            }
        }

        mWaitLatch.await(timeout, unit);

        if (mIsCancelled) {
            throw new CancellationException("cancel has been requested");
        }

        if (jobEvent == null) {
            throw new TimeoutException("job result await timed out");
        }

        return jobEvent;
    }

    public int getJobId() {
        return mJobId;
    }
}
