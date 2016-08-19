package com.be.android.library.worker.base;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class JobEventInvocationHandlerProvider implements InvocationHandlerProvider {

    @Override
    public InvocationHandler createInvocationHandler(Method method) {
        return new JobEventInvocationHandler(method);
    }

    @Override
    public boolean canCreateInvocationHandler(Annotation annotation) {
        return annotation.annotationType().equals(JobEventInvocationHandler.ANNOTATION_TYPE);
    }
}
