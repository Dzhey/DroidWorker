package com.be.android.library.worker.base;

import com.be.android.library.worker.interfaces.Job;
import com.be.android.library.worker.interfaces.JobEventListener;
import com.be.android.library.worker.interfaces.JobEventObservable;

public abstract class JobObservable implements Job, JobEventObservable {

    private final JobEventObservable mObservable;

    protected JobObservable(JobEventObservable observable) {
        mObservable = observable;
    }

    @Override
    public void removeJobEventListeners() {
        mObservable.removeJobEventListeners();
    }

    @Override
    public void reset() {
        removeJobEventListeners();
    }

    @Override
    public boolean hasJobEventListener(JobEventListener listener) {
        return mObservable.hasJobEventListener(listener);
    }

    @Override
    public boolean hasJobEventListener(String listenerTag) {
        return mObservable.hasJobEventListener(listenerTag);
    }

    @Override
    public JobEventListener findJobEventListener(String listenerTag) {
        return mObservable.findJobEventListener(listenerTag);
    }

    @Override
    public void addJobEventListener(JobEventListener listener) {
        mObservable.addJobEventListener(listener);
    }

    @Override
    public void addJobEventListener(String tag, JobEventListener listener) {
        mObservable.addJobEventListener(tag, listener);
    }

    @Override
    public boolean removeJobEventListener(JobEventListener listener) {
        return mObservable.removeJobEventListener(listener);
    }

    @Override
    public boolean removeJobEventListener(String tag) {
        return mObservable.removeJobEventListener(tag);
    }

    @Override
    public void notifyJobEvent(JobEvent result) {
        mObservable.notifyJobEvent(result);
    }
}
