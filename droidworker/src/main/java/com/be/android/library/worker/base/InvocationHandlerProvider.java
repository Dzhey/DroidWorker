package com.be.android.library.worker.base;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public interface InvocationHandlerProvider {
    InvocationHandler createInvocationHandler(Method method);
    boolean canCreateInvocationHandler(Annotation annotation);
}
