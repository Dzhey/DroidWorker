package com.be.android.library.worker.base;

import java.lang.reflect.InvocationTargetException;

interface EventHandlerInvoker {
    boolean isApplicable(JobEvent event);
    boolean isFitEvent(JobEvent event);
    Object invoke(Object target, JobEvent event) throws InvocationTargetException, IllegalAccessException;
}
