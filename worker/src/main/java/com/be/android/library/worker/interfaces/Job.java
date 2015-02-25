package com.be.android.library.worker.interfaces;

import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.base.JobStatus;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public interface Job extends Callable<JobEvent>, Runnable, JobEventObservable {

    /**
     * If set then job with group {@link com.be.android.library.worker.controllers.JobManager#JOB_GROUP_UNIQUE}
     * should not be enqueued when there are no any free threads.
     * In such case an extra executor should be allocated.
     * <br />
     * <br />
     * Any forked jobs with group {@link com.be.android.library.worker.controllers.JobManager#JOB_GROUP_UNIQUE}
     * has to have this flag to ensure correct execution flow.
     */
    public static final int EXECUTION_FLAG_FORCE_EXECUTE = 0x1;

    int getExecutionFlags();

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

    void setPaused(Object pauseToken, boolean isPaused);

    boolean isPaused();
}
