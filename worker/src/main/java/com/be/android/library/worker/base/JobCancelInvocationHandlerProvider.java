package com.be.android.library.worker.base;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class JobCancelInvocationHandlerProvider implements InvocationHandlerProvider {

    @Override
    public InvocationHandler createInvocationHandler(Method method) {
        return new JobCancelInvocationHandler(method);
    }

    @Override
    public boolean canCreateInvocationHandler(Annotation annotation) {
        return annotation.annotationType().equals(JobCancelInvocationHandler.ANNOTATION_TYPE);
    }
}
