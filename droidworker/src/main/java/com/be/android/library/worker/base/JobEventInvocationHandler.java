package com.be.android.library.worker.base;

import com.be.android.library.worker.annotations.OnJobEvent;
import com.be.android.library.worker.interfaces.Job;

import java.lang.reflect.Method;

public class JobEventInvocationHandler extends BaseInvocationHandler {

    public static Class<OnJobEvent> ANNOTATION_TYPE = OnJobEvent.class;

    private JobStatus[] mPendingStatus;
    private int[] mPendingEventCode;
    private String[] mPendingTags;
    private Class<?> mPendingJobType;

    public JobEventInvocationHandler(Method method) {
        super(method);

        OnJobEvent annotation = method.getAnnotation(ANNOTATION_TYPE);
        if (annotation == null) {
            throw new RuntimeException(String.format(
                    "no annotation '%s' found on method '%s'",
                    ANNOTATION_TYPE.getName(), method.getName()));
        }

        if (annotation.jobType().equals(Job.class) == false
                && annotation.value().equals(Job.class) == false) {

            throw new RuntimeException(String.format(
                    "Inconsistent value:'%s' and jobType:'%s' on annotation:'%s' " +
                            "with method:'%s'; please consider to use one of two variants",
                    annotation.value(),
                    annotation.jobType(),
                    ANNOTATION_TYPE.getName(),
                    method.getName()
            ));
        }

        if (annotation.value().equals(Job.class) == false) {
            mPendingJobType = annotation.value();
        } else {
            mPendingJobType = annotation.jobType();
        }
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
}
