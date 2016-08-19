package com.be.android.library.worker.base;

import android.content.Context;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HierarchyViewer {

    private static final Map<String, List<InvocationHandler>> sHandlersMap;
    private final List<InvocationHandlerProvider> mProviders;
    private final String mPackageName;

    static {
        sHandlersMap = new HashMap<String, List<InvocationHandler>>();
    }

    public HierarchyViewer(Context context) {
        mProviders = new ArrayList<InvocationHandlerProvider>();
        mPackageName = context.getPackageName();
    }

    public void registerInvocationHandlerProvider(InvocationHandlerProvider provider) {
        mProviders.add(provider);
    }

    public void removeInvocationHandlerProvider(InvocationHandlerProvider provider) {
        mProviders.remove(provider);
    }

    protected InvocationHandlerProvider getInvocationHandlerProvider(Class<?> type, Method method) {
        for (InvocationHandlerProvider provider : mProviders) {
            for (Annotation annotation : method.getDeclaredAnnotations()) {
                if (provider.canCreateInvocationHandler(annotation)) {
                    return provider;
                }
            }
        }

        return null;
    }

    public List<InvocationHandler> fetchInvocationHandlers(Class<?> type) {
        if (sHandlersMap.containsKey(type.getName())) {
            return sHandlersMap.get(type.getName());
        }

        Class<?> lookupType = type;
        List<InvocationHandler> handlers = new ArrayList<InvocationHandler>();
        while (Object.class.equals(lookupType) == false) {
            final Package packageInfo = lookupType.getPackage();
            if (packageInfo != null && packageInfo.getName().startsWith(mPackageName) == false) {
                break;
            }

            for (Method method : lookupType.getDeclaredMethods()) {
                InvocationHandlerProvider provider = getInvocationHandlerProvider(type, method);
                if (provider != null) {
                    handlers.add(provider.createInvocationHandler(method));
                }
            }

            lookupType = lookupType.getSuperclass();
        }

        if (handlers.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "specified type '%s' declared no any event handlers", type));
        }

        sHandlersMap.put(type.getName(), handlers);

        return handlers;
    }
}
