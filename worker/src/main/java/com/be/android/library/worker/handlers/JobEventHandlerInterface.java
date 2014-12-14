package com.be.android.library.worker.handlers;

import com.be.android.library.worker.interfaces.Job;

public interface JobEventHandlerInterface {
    boolean isPending(int jobId);
    boolean isPending(String... jobTags);
    boolean addPendingJob(int jobId);
    int submitJob(Job job);
}
