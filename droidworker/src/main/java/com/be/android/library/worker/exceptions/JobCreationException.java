package com.be.android.library.worker.exceptions;

public class JobCreationException extends RuntimeException {

    public JobCreationException(Throwable throwable) {
        super(throwable);
    }

    public JobCreationException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public JobCreationException(String detailMessage) {
        super(detailMessage);
    }

    public JobCreationException() {
    }
}
