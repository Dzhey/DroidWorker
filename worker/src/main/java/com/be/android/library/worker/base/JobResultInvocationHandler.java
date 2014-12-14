package com.be.android.library.worker.base;

import com.be.android.library.worker.annotations.OnJobResult;

import java.lang.reflect.Method;

public class JobResultInvocationHandler extends BaseInvocationHandler {

    public static Class<OnJobResult> ANNOTATION_TYPE = OnJobResult.class;

    private Method mMethod;
    private JobStatus[] mPendingStatus;
    private int[] mPendingEventCode;
    private String[] mPendingTags;
    private Class<?> mPendingJobType;

    public JobResultInvocationHandler(Method method) {
        mMethod = method;

        OnJobResult annotation = mMethod.getAnnotation(OnJobResult.class);
        if (annotation == null) {
            throw new RuntimeException(String.format(
                    "no annotation '%s' found on method '%s'",
                    OnJobResult.class.getName(), method.getName()));
        }

        mPendingStatus = new JobStatus[] { JobStatus.OK, JobStatus.FAILED, JobStatus.CANCELLED };
        mPendingJobType = annotation.jobType();
        mPendingEventCode = annotation.eventCode();
        mPendingTags = annotation.jobTags();
    }

    @Override
    protected Class<?> getPendingJobType() {
        return null;
    }

    @Override
    protected JobStatus[] getPendingStatus() {
        return mPendingStatus;
    }

    @Override
    protected int[] getPendingEventCode() {
        return null;
    }

    @Override
    protected String[] getPendingTags() {
        return null;
    }

    @Override
    protected void invokeEventHandler(Object receiver, JobEvent event) throws Exception {
        mMethod.invoke(receiver, event);
    }
}
