package com.be.android.library.worker.base;

import com.be.android.library.worker.annotations.OnJobEvent;

import java.lang.reflect.Method;

public class JobEventInvocationHandler extends BaseInvocationHandler {

    public static Class<OnJobEvent> ANNOTATION_TYPE = OnJobEvent.class;

    private Method mMethod;
    private JobStatus[] mPendingStatus;
    private int[] mPendingEventCode;
    private String[] mPendingTags;
    private Class<?> mPendingJobType;

    public JobEventInvocationHandler(Method method) {
        mMethod = method;

        OnJobEvent annotation = mMethod.getAnnotation(OnJobEvent.class);
        if (annotation == null) {
            throw new RuntimeException(String.format(
                    "no annotation '%s' found on method '%s'",
                    OnJobEvent.class.getName(), method.getName()));
        }

        mPendingJobType = annotation.jobType();
        mPendingStatus = annotation.jobStatus();
        mPendingEventCode = annotation.eventCode();
        mPendingTags = annotation.jobTags();
    }

    @Override
    protected Class<?> getPendingJobType() {
        return mPendingJobType;
    }

    @Override
    protected JobStatus[] getPendingStatus() {
        return mPendingStatus;
    }

    @Override
    protected int[] getPendingEventCode() {
        return mPendingEventCode;
    }

    @Override
    protected String[] getPendingTags() {
        return mPendingTags;
    }

    @Override
    protected void invokeEventHandler(Object receiver, JobEvent event) throws Exception {
        mMethod.invoke(receiver, event);
    }
}
