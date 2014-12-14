package com.be.android.library.worker.exceptions;

public class JobExecutionException extends Exception {
    public JobExecutionException() {
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
}
