package com.be.android.library.worker.models;

import java.lang.ref.WeakReference;

public final class JobListenerEntry<T> {

    private WeakReference<T> mListenerReference;
    private String listenerTag;

    public JobListenerEntry(T listener, String listenerTag) {
        this.mListenerReference = new WeakReference<T>(listener);
        this.listenerTag = listenerTag;
    }

    public WeakReference<T> getListenerReference() {
        return mListenerReference;
    }

    public String getListenerTag() {
        return listenerTag;
    }
}
