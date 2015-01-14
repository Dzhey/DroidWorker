package com.be.android.library.worker.base;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class JobFailureInvocationHandlerProvider implements InvocationHandlerProvider {

    @Override
    public InvocationHandler createInvocationHandler(Method method) {
        return new JobFailureInvocationHandler(method);
    }

    @Override
    public boolean canCreateInvocationHandler(Annotation annotation) {
        return annotation.annotationType().equals(JobFailureInvocationHandler.ANNOTATION_TYPE);
    }
}
