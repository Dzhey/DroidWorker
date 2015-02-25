package com.be.android.library.worker.handlers;

import com.be.android.library.worker.interfaces.Job;
import com.be.android.library.worker.util.JobSelector;

public interface JobEventHandlerInterface {
    boolean isPending(int jobId);
    boolean isPending(JobSelector selector);
    boolean isPendingAll(JobSelector selector);
    boolean addPendingJob(int jobId);
    int submitJob(Job job);
}
