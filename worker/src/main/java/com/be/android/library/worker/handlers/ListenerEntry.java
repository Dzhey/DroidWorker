package com.be.android.library.worker.handlers;

import com.be.android.library.worker.base.InvocationHandler;

import java.lang.ref.WeakReference;
import java.util.List;

class ListenerEntry {
    WeakReference<Object> mListenerObjectRef;
    List<InvocationHandler> mInvocationHandlers;

    ListenerEntry(WeakReference<Object> mListenerObjectRef,
                  List<InvocationHandler> invocationHandlers) {

        this.mListenerObjectRef = mListenerObjectRef;
        this.mInvocationHandlers = invocationHandlers;
    }

    public WeakReference<Object> getListenerObjectRef() {
        return mListenerObjectRef;
    }

    public List<InvocationHandler> getInvocationHandlers() {
        return mInvocationHandlers;
    }
}