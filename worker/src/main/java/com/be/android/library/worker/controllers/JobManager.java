package com.be.android.library.worker.controllers;

import android.os.Handler;

import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.base.JobStatus;
import com.be.android.library.worker.interfaces.Job;
import com.be.android.library.worker.interfaces.JobEventListener;
import com.be.android.library.worker.interfaces.JobEventObservable;
import com.be.android.library.worker.util.JobFutureResult;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class JobManager implements JobEventObservable {

    public static final int JOB_ID_UNSPECIFIED = -1;
    public static final int JOB_GROUP_DEFAULT = 0;
    public static final int JOB_GROUP_UNIQUE = -1;
    public static final int JOB_GROUP_DEDICATED = -2;

    private final JobEventObservable mJobObservable;
    private final ConcurrentLinkedQueue<Job> mJobs;
    private final AtomicInteger mJobIdCounter;
    private final Handler mHandler;

    private static JobManager instance;
    private static final Object MUTEX = new Object();

    private final JobEventListener mJobEventListener = new JobEventListener() {
        @Override
        public void onJobEvent(final JobEvent event) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    handleJobEvent(event);
                }
            });
        }
    };

    public static JobManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("job manager instance is not defined; call init() first");
        }

        return instance;
    }

    public static void init(JobManager jobManager) {
        synchronized (MUTEX) {
            if (instance != null) {
                throw new IllegalStateException("job manager singleton is already initialized");
            }
            instance = jobManager;
        }
    }

    protected JobManager() {
        mJobObservable = new JobEventObservableImpl();
        mJobs = new ConcurrentLinkedQueue<Job>();
        mJobIdCounter = new AtomicInteger(0);
        mHandler = new Handler();
    }

    public JobFutureResult submitJobForResult(Job job) {
        final JobFutureResult pendingResult = new JobFutureResult(job);

        submitJob(job);

        return pendingResult;
    }

    public int submitJob(Job job) {
        checkJobPreconditions(job);

        final int jobId = mJobIdCounter.incrementAndGet();
        job.setJobId(jobId);
        job.addJobEventListener(mJobEventListener);

        mJobs.add(job);

        submitJobImpl(job);

        return jobId;
    }

    protected abstract void submitJobImpl(Job job);

    public Job findJob(int jobId) {
        for (Job job : mJobs) {
            if (job.getJobId() == jobId) {
                return job;
            }
        }

        return null;
    }

    public Job findJob(String... jobTags) {
        if (jobTags.length == 0) return null;

        for (Job job : mJobs) {
            if (job.hasTags(jobTags)) {
                return job;
            }
        }

        return null;
    }

    public Job findJob(Collection<String> jobTags) {
        if (jobTags.isEmpty()) return null;

        for (Job job : mJobs) {
            if (job.hasTags(jobTags)) {
                return job;
            }
        }

        return null;
    }

    private void handleJobEvent(JobEvent event) {
        Job job = findJob(event.getJobId());

        if (event.isJobFinished()) {
            if (job != null) {
                job.removeJobEventListener(mJobEventListener);
            }
            discardJob(event.getJobId());
        }

        notifyJobEvent(event);
    }

    public boolean isJobCancelled(int jobId) {
        Job job = findJob(jobId);

        return job != null && job.isCancelled();
    }

    public boolean cancelJob(int jobId) {
        Job job = findJob(jobId);
        if (job != null && job.isCancelled() == false) {
            job.cancel();

            return true;
        }

        return false;
    }

    public boolean discardJob(int jobId) {
        Iterator<Job> iter = mJobs.iterator();
        while (iter.hasNext()) {
            Job next = iter.next();

            if (next.getJobId() == jobId) {
                iter.remove();
                return true;
            }
        }

        return false;
    }

    private void checkJobPreconditions(Job job) {
        if (job == null) {
            throw new IllegalArgumentException("job is null");
        }
        if (job.getJobId() != JOB_ID_UNSPECIFIED || job.getStatus() != JobStatus.PENDING) {
            throw new IllegalArgumentException(
                    "job is already submitted or it's status manually changed");
        }
    }

    @Override
    public boolean hasJobEventListener(JobEventListener listener) {
        return mJobObservable.hasJobEventListener(listener);
    }

    @Override
    public boolean hasJobEventListener(String listenerTag) {
        return mJobObservable.hasJobEventListener(listenerTag);
    }

    @Override
    public JobEventListener findJobEventListener(String listenerTag) {
        return mJobObservable.findJobEventListener(listenerTag);
    }

    @Override
    public void addJobEventListener(JobEventListener listener) {
        mJobObservable.addJobEventListener(listener);
    }

    @Override
    public void addJobEventListener(String tag, JobEventListener listener) {
        mJobObservable.addJobEventListener(tag, listener);
    }

    @Override
    public boolean removeJobEventListener(JobEventListener listener) {
        return mJobObservable.removeJobEventListener(listener);
    }

    @Override
    public boolean removeJobEventListener(String tag) {
        return mJobObservable.removeJobEventListener(tag);
    }

    @Override
    public void notifyJobEvent(JobEvent result) {
        mJobObservable.notifyJobEvent(result);
    }

    @Override
    public void removeJobEventListeners() {
        mJobObservable.removeJobEventListeners();
    }
}
