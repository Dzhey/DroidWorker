package com.be.android.library.worker.exceptions;

import com.be.android.library.worker.base.JobEvent;

public class JobExecutionException extends Exception {

    private JobEvent mJobEvent;

    public JobExecutionException() {
    }

    public JobExecutionException(JobEvent result) {
        mJobEvent = result;
    }

    public JobExecutionException(String detailMessage) {
        super(detailMessage);
    }

    public JobExecutionException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public JobExecutionException(Throwable throwable) {
        super(throwable);
    }

    public JobEvent getJobEvent() {
        return mJobEvent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JobExecutionException that = (JobExecutionException) o;

        if (mJobEvent != null ? !mJobEvent.equals(that.mJobEvent) : that.mJobEvent != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return mJobEvent != null ? mJobEvent.hashCode() : 0;
    }
}
