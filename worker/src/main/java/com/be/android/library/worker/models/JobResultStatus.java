package com.be.android.library.worker.models;

import com.be.android.library.worker.base.JobEvent;

public enum JobResultStatus {
    OK, FAILED, CANCELLED;

    public static int getResultCode(JobResultStatus status) {
        switch (status) {
            case OK:
                return JobEvent.EVENT_CODE_OK;

            case FAILED:
                return JobEvent.EVENT_CODE_FAILED;

            case CANCELLED:
                return JobEvent.EVENT_CODE_CANCELLED;

            default:
                throw new RuntimeException(String.format(
                        "unexpected job result status '%s'", status));
        }
    }
}
