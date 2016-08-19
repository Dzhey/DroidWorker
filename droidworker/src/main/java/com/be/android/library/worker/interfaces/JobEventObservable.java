package com.be.android.library.worker.interfaces;

import com.be.android.library.worker.base.JobEvent;

public interface JobEventObservable {

    boolean hasJobEventListener(JobEventListener listener);

    boolean hasJobEventListener(String listenerTag);

    JobEventListener findJobEventListener(String listenerTag);

    void addJobEventListener(JobEventListener listener);

    void addJobEventListener(String tag, JobEventListener listener);

    boolean removeJobEventListener(JobEventListener listener);

    boolean removeJobEventListener(String tag);

    void notifyJobEvent(JobEvent result);

    void removeJobEventListeners();
}
