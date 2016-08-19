package com.be.android.library.worker.interfaces;

import com.be.android.library.worker.base.JobEvent;

public interface JobEventListener {
    void onJobEvent(JobEvent event);
}
