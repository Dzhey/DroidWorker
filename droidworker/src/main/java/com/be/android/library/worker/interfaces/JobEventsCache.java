package com.be.android.library.worker.interfaces;

import com.be.android.library.worker.base.JobEvent;

public interface JobEventsCache {
    public JobEvent getEvent(String key);
    public void putEvent(JobEvent event, String key, CacheEntryDescriptor descriptor);
    public void evictEvent(String key);
    public void evictAll();
}
