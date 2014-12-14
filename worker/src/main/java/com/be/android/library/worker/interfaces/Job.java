package com.be.android.library.worker.interfaces;

import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.base.JobStatus;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public interface Job extends Callable<JobEvent>, Runnable, JobEventObservable {
    void setPriority(int priority);

    int getPriority();

    boolean isJobIdAssigned();

    boolean isFinished();

    void setJobId(int jobId);

    int getJobId();

    void setStatus(JobStatus status);

    JobStatus getStatus();

    void cancel();

    boolean isCancelled();

    void setPayload(Object payload);

    boolean hasPayload();

    Object getPayload();

    void addTag(String tag);

    void addTags(String... tags);

    void addTags(Collection<String> tags);

    List<String> getTags();

    boolean hasTag(String tag);

    boolean hasTags(String... tags);

    boolean hasTags(Collection<String> tags);

    void setGroupId(int groupId);

    int getGroupId();

    void reset();

    JobEvent execute();

    Future<JobEvent> getFutureResult();

    void setPaused(boolean isPaused);

    boolean isPaused();
}

