package com.be.android.library.worker.base;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class JobSuccessInvocationHandlerProvider implements InvocationHandlerProvider {

    @Override
    public InvocationHandler createInvocationHandler(Method method) {
        return new JobSuccessInvocationHandler(method);
    }

    @Override
    public boolean canCreateInvocationHandler(Annotation annotation) {
        return annotation.annotationType().equals(JobSuccessInvocationHandler.ANNOTATION_TYPE);
    }
}
