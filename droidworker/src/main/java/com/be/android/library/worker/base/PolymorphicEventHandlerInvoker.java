package com.be.android.library.worker.base;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PolymorphicEventHandlerInvoker implements EventHandlerInvoker {

    private final Method mMethod;
    private final Class<?>[] mMethodParamTypes;

    public PolymorphicEventHandlerInvoker(Method method) {
        mMethod = method;
        mMethodParamTypes = method.getParameterTypes();
    }

    @Override
    public boolean isApplicable(JobEvent event) {
        if (mMethodParamTypes.length > 1) {
            return false;

        } else if (mMethodParamTypes.length == 0) {
            return true;
        }

        if (mMethodParamTypes[0] == JobEvent.class) {
            return true;
        }

        return mMethodParamTypes[0].isAssignableFrom(event.getClass());
    }

    @Override
    public boolean isFitEvent(JobEvent event) {
        if (mMethodParamTypes.length > 1 || mMethodParamTypes.length == 0) {
            return false;
        }

        return mMethodParamTypes[0].isAssignableFrom(event.getClass());
    }

    @Override
    public Object invoke(Object target, JobEvent event) throws InvocationTargetException, IllegalAccessException {
        if (mMethodParamTypes.length == 0) {
            return mMethod.invoke(target);
        } else {
            return mMethod.invoke(target, event);
        }
    }
}
