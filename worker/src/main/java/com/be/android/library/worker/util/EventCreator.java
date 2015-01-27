package com.be.android.library.worker.util;

import com.be.android.library.worker.base.JobEvent;

public interface EventCreator<T extends JobEvent> {
    public T createInstance();
}
