package com.be.android.library.worker.base;

public interface InvocationHandler {
    boolean canApply(Object receiver, JobEvent event);
    boolean isFitEvent(Object receiver, JobEvent event);
    boolean apply(Object receiver, JobEvent event) throws Exception;
}
