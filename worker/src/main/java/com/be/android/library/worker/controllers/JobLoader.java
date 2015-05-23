package com.be.android.library.worker.controllers;

import android.util.Log;

import com.be.android.library.worker.base.JobConfigurator;
import com.be.android.library.worker.base.JobStatusLock;
import com.be.android.library.worker.handlers.JobEventHandlerInterface;
import com.be.android.library.worker.interfaces.Job;
import com.be.android.library.worker.util.JobSelector;

import java.lang.ref.WeakReference;

public class JobLoader {

    private static final String LOG_TAG = JobLoader.class.getSimpleName();

    private static final String EXTRA_LOADER_TAG = "com.be.android.library.worker.extras.LOADER_TAG";

    public interface JobLoaderCallbacks {
        Job onCreateJob(String attachTag);
    }

    private final JobManager mJobManager;
    private final String mAttachTag;
    private WeakReference<JobEventHandlerInterface> mEventHandler;
    private WeakReference<JobLoaderCallbacks> mCallbacks;

    protected JobLoader(JobManager jobManager,
                        JobEventHandlerInterface eventHandler,
                        String attachTag,
                        JobLoaderCallbacks callbacks) {
        mJobManager = jobManager;

        mAttachTag = attachTag;
        setEventHandler(eventHandler);
        setCallbacks(callbacks);
    }

    final void setEventHandler(JobEventHandlerInterface eventHandler) {
        mEventHandler = new WeakReference<JobEventHandlerInterface>(eventHandler);
    }

    final void setCallbacks(JobLoaderCallbacks callbacks) {
        mCallbacks = new WeakReference<JobLoaderCallbacks>(callbacks);
    }

    public int requestLoad() {
        final JobEventHandlerInterface eventHandler = mEventHandler.get();
        final JobLoaderCallbacks callbacks = mCallbacks.get();

        if (eventHandler == null || callbacks == null) {
            return JobManager.JOB_ID_UNSPECIFIED;
        }

        Job job = mJobManager.findJob(JobSelector.forJobTags(EXTRA_LOADER_TAG, mAttachTag));

        if (job != null && job.hasParams()) {
            JobStatusLock lock = job.acquireStatusLock();
            try {
                lock.lock();

                if (!job.isFinished() && !job.isCancelled()) {
                    if (eventHandler.addPendingJob(job.getJobId())) {
                        return job.getJobId();
                    }
                }

            } catch (InterruptedException e) {
                Log.d(LOG_TAG, "requestLoad() status lock interrupted");

            } finally {
                lock.release();
            }
        }

        job = callbacks.onCreateJob(mAttachTag);
        if (job.hasParams()) {
            throw new IllegalArgumentException("job is already set up");
        }
        job.setup().addTag(mAttachTag).apply();

        return eventHandler.submitJob(job);
    }
}
