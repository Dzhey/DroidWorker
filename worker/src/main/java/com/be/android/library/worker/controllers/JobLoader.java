package com.be.android.library.worker.controllers;

import android.os.Bundle;

import com.be.android.library.worker.base.JobStatusLock;
import com.be.android.library.worker.handlers.JobEventHandlerInterface;
import com.be.android.library.worker.interfaces.Job;
import com.be.android.library.worker.util.JobSelector;

import java.lang.ref.WeakReference;

public class JobLoader {

    public interface JobLoaderCallbacks {
        Job onCreateJob(String attachTag, Bundle data);
    }

    private final JobManager mJobManager;
    private final String mAttachTag;
    private final JobSelector mJobSelector;
    private WeakReference<JobEventHandlerInterface> mEventHandler;
    private WeakReference<JobLoaderCallbacks> mCallbacks;

    protected JobLoader(JobManager jobManager,
                        JobEventHandlerInterface eventHandler,
                        String attachTag,
                        JobLoaderCallbacks callbacks) {
        mJobManager = jobManager;

        mAttachTag = attachTag;
        mJobSelector = JobSelector.forJobTags(mAttachTag);
        setEventHandler(eventHandler);
        setCallbacks(callbacks);
    }

    final void setEventHandler(JobEventHandlerInterface eventHandler) {
        mEventHandler = new WeakReference<JobEventHandlerInterface>(eventHandler);
    }

    final void setCallbacks(JobLoaderCallbacks callbacks) {
        mCallbacks = new WeakReference<JobLoaderCallbacks>(callbacks);
    }

    public int requestLoad(Bundle data) {
        final JobEventHandlerInterface eventHandler = mEventHandler.get();
        final JobLoaderCallbacks callbacks = mCallbacks.get();

        if (eventHandler == null || callbacks == null) {
            return JobManager.JOB_ID_UNSPECIFIED;
        }

        Job job = mJobManager.findJob(mJobSelector);

        if (job != null && job.hasParams()) {
            final JobStatusLock lock = job.acquireStatusLock();
            try {
                if (!job.isCancelled()) {
                    if (job.isFinished() && eventHandler.isPending(job.getJobId())) {
                        // Job has finished, but job result is still enqueued
                        return job.getJobId();
                    }

                    if (!job.isFinished()) {
                        // Attach handler to running job
                        if (eventHandler.addPendingJob(job.getJobId())) {
                            return job.getJobId();

                        } else {
                            throw new IllegalStateException(
                                    "job %s has been unexpectedly removed from JobManager");
                        }
                    }
                }
            } finally {
                lock.release();
            }
        }

        job = callbacks.onCreateJob(mAttachTag, data);
        if (job.hasParams()) {
            throw new IllegalArgumentException("job is already set up");
        }
        job.setup().addTag(mAttachTag).apply();

        return eventHandler.submitJob(job);
    }
}
