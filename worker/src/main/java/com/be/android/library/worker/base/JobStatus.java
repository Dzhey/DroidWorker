package com.be.android.library.worker.base;

import com.be.android.library.worker.models.JobResultStatus;

public enum JobStatus {
    PENDING, ENQUEUED, SUBMITTED, IN_PROGRESS, OK, FAILED, CANCELLED;

    static JobStatus fromJobResultStatus(JobResultStatus status) {
        switch (status) {
            case OK:
                return JobStatus.OK;
            case FAILED:
                return JobStatus.FAILED;
            default:
                throw new RuntimeException(String.format(
                        "can't map job result status '%s' on '%s'",
                        status, JobStatus.class.getSimpleName()));
        }
    }


}
