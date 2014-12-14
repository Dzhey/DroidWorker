package com.be.android.library.worker.base;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class JobResultInvocationHandlerProvider implements InvocationHandlerProvider {

    @Override
    public InvocationHandler createInvocationHandler(Method method) {
        return new JobResultInvocationHandler(method);
    }

    @Override
    public boolean canCreateInvocationHandler(Annotation annotation) {
        return annotation.annotationType().equals(JobResultInvocationHandler.ANNOTATION_TYPE);
    }
}
