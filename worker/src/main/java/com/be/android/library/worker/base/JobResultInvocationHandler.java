package com.be.android.library.worker.base;

import com.be.android.library.worker.annotations.OnJobResult;

import java.lang.reflect.Method;

public class JobResultInvocationHandler extends BaseInvocationHandler {

    public static Class<OnJobResult> ANNOTATION_TYPE = OnJobResult.class;

    private JobStatus[] mPendingStatus;
    private int[] mPendingEventCode;
    private String[] mPendingTags;
    private Class<?> mPendingJobType;

    public JobResultInvocationHandler(Method method) {
        super(method);

        OnJobResult annotation = method.getAnnotation(ANNOTATION_TYPE);
        if (annotation == null) {
            throw new RuntimeException(String.format(
                    "no annotation '%s' found on method '%s'",
                    ANNOTATION_TYPE.getName(), method.getName()));
        }

        mPendingStatus = new JobStatus[] { JobStatus.OK, JobStatus.FAILED, JobStatus.CANCELLED };
        mPendingJobType = annotation.jobType();
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
}