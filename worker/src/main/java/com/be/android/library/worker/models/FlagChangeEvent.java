package com.be.android.library.worker.models;

import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.base.JobStatus;

public class FlagChangeEvent extends JobEvent {

    private final Flag mFlag;

    public FlagChangeEvent(JobStatus jobStatus, Flag flag) {
        super(JobEvent.EVENT_CODE_UPDATE, JobEvent.EXTRA_CODE_FLAG_STATUS_CHANGED, jobStatus);

        mFlag = flag;
    }

    public Flag getFlag() {
        return mFlag;
    }
}
