package com.be.android.library.worker.models;

import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.base.JobStatus;

public class ProgressUpdateEvent extends JobEvent {

    private final float mProgress;

    public ProgressUpdateEvent(float progress, JobStatus status) {
        super(EVENT_CODE_UPDATE, EXTRA_CODE_PROGRESS_UPDATE, status);

        mProgress = progress;
    }

    public float getProgress() {
        return mProgress;
    }
}
