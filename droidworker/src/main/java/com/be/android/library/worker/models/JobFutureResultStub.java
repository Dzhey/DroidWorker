package com.be.android.library.worker.models;

import com.be.android.library.worker.base.JobEvent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class JobFutureResultStub implements Future<JobEvent> {

    private JobEvent jobEvent;

    public JobFutureResultStub(JobEvent jobEvent) {
        this.jobEvent = jobEvent;
    }

    @Override
    public boolean cancel(boolean b) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public JobEvent get() throws InterruptedException, ExecutionException {
        return jobEvent;
    }

    @Override
    public JobEvent get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        return jobEvent;
    }
}
