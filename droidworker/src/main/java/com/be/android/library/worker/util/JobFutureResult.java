package com.be.android.library.worker.util;

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
    private JobEvent mJobEvent;
    private JobEvent mResultEvent;
    private final Object mMutex = new Object();
    private final CountDownLatch mWaitLatch = new CountDownLatch(1);
    private final JobManager mJobManager;
    private JobEventFilter mEventFilter;

    private final JobEventListener mJobEventListener =
            new JobEventListener() {
                @Override
                public void onJobEvent(JobEvent event) {
                    if (mJobId == JobManager.JOB_ID_UNSPECIFIED
                            && event.getEventCode() == JobEvent.EXTRA_CODE_STATUS_CHANGED) {

                        mJobId = event.getJobParams().getJobId();

                        if (mIsCancelled) {
                            mJobManager.cancelJob(mJobId);
                            mWaitLatch.countDown();
                            return;
                        }
                    }

                    if (!handleJobEvent(event)) {
                        if (event.isJobFinished()) {
                            synchronized (mMutex) {
                                mResultEvent = event;
                            }
                            mWaitLatch.countDown();
                        }

                        return;
                    }

                    synchronized (mMutex) {
                        if (event.isJobFinished()) {
                            mResultEvent = event;
                        }
                        mJobEvent = event;
                    }
                    mWaitLatch.countDown();
                }
            };

    public JobFutureResult(Job job) {
        this(job, JobManager.getInstance());
    }

    public JobFutureResult(Job job, JobManager jobManager) {
        if (job.getStatus() != JobStatus.PENDING) {
            throw new IllegalStateException(String.format("Can't create pending " +
                    "future result for submitted job; \"%s\"", job));
        }

        mJobManager = jobManager;

        job.addJobEventListener(mJobEventListener);
    }

    /**
     * @param event event to process
     * @return true to obtain provided event as result
     */
    protected boolean handleJobEvent(JobEvent event) {
        if (mEventFilter != null) {
            return mEventFilter.apply(event);
        }

        return event.isJobFinished();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (mIsCancelled) return true;

        mIsCancelled = true;

        if (mJobId != JobManager.JOB_ID_UNSPECIFIED) {
            mWaitLatch.countDown();

            return mJobManager.cancelJob(mJobId);
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
            return mJobEvent != null;
        }
    }

    /**
     * {@inheritDoc}
     * @return expected JobEvent or null if job was finished and no one event was handled
     * @throws InterruptedException as per {@link Future#get()}
     * @throws ExecutionException as per {@link Future#get()}
     */
    @Override
    public JobEvent get() throws InterruptedException, ExecutionException {
        synchronized (mMutex) {
            if (mJobEvent != null) {
                return mJobEvent;
            }
        }

        mWaitLatch.await();

        return mJobEvent;
    }

    /**
     * @param timeout as per {@link Future#get(long, TimeUnit)}
     * @param unit as per {@link Future#get(long, TimeUnit)}
     * @return expected JobEvent or null if job was finished and no one event was handled
     * @throws InterruptedException as per {@link Future#get(long, TimeUnit)}
     * @throws ExecutionException as per {@link Future#get(long, TimeUnit)}
     * @throws TimeoutException as per {@link Future#get(long, TimeUnit)}
     */
    @Override
    public JobEvent get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {

        synchronized (mMutex) {
            if (mJobEvent != null) {
                return mJobEvent;
            }
        }

        mWaitLatch.await(timeout, unit);

        if (mIsCancelled) {
            throw new CancellationException("cancel has been requested");
        }

        return mJobEvent;
    }

    /**
     * @return result event if got any
     */
    public JobEvent getResultEvent() {
        return mResultEvent;
    }

    public int getJobId() {
        return mJobId;
    }

    public JobEventFilter getEventFilter() {
        return mEventFilter;
    }

    public void setEventFilter(JobEventFilter eventFilter) {
        mEventFilter = eventFilter;
    }
}
