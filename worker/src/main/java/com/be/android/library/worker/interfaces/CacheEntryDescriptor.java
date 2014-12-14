package com.be.android.library.worker.interfaces;

import com.be.android.library.worker.base.JobEvent;

public interface CacheEntryDescriptor {
    public static final int TIMEOUT_NONE = -1;

    public int getJobEventSize(JobEvent event);
    public int getExpirationTimeoutMillis();
}
