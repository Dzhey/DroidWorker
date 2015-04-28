package com.be.android.library.worker.controllers;

import com.be.android.library.worker.handlers.JobEventHandlerInterface;
import com.be.android.library.worker.interfaces.Job;
import com.be.android.library.worker.util.JobSelector;

import java.lang.ref.WeakReference;

public class JobLoader {

    public interface JobLoaderCallbacks {
        Job onCreateJob(String attachTag);
    }

    private final String mAttachTag;
    private WeakReference<JobEventHandlerInterface> mEventHandler;
    private WeakReference<JobLoaderCallbacks> mCallbacks;

    protected JobLoader(JobEventHandlerInterface eventHandler,
                        String attachTag, JobLoaderCallbacks callbacks) {

        this.mAttachTag = attachTag;
        setEventHandler(eventHandler);
        setCallbacks(callbacks);
    }

    void setEventHandler(JobEventHandlerInterface eventHandler) {
        mEventHandler = new WeakReference<JobEventHandlerInterface>(eventHandler);
    }

    void setCallbacks(JobLoaderCallbacks callbacks) {
        mCallbacks = new WeakReference<JobLoaderCallbacks>(callbacks);
    }

    public int requestLoad() {
        final JobEventHandlerInterface eventHandler = mEventHandler.get();
        final JobLoaderCallbacks callbacks = mCallbacks.get();

        if (eventHandler == null || callbacks == null) {
            return JobManager.JOB_ID_UNSPECIFIED;
        }

        Job job = JobManager.getInstance().findJob(JobSelector.forJobTags(mAttachTag));

        if (job != null && job.isFinished() == false && job.isCancelled() == false) {
            if (eventHandler.addPendingJob(job.getJobId())) {
                return job.getJobId();
            }
        }

        job = callbacks.onCreateJob(mAttachTag);
        if (!job.getParams().hasTag(mAttachTag)) {
            throw new IllegalStateException(
                    String.format("requested job has to have tag '%s'", mAttachTag));
        }

        return eventHandler.submitJob(job);
    }
}
