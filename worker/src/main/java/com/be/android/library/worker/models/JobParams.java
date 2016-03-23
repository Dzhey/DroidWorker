package com.be.android.library.worker.models;

import com.be.android.library.worker.interfaces.FlagsProvider;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface JobParams extends FlagsProvider {

    /**
     * If set then job with group {@link com.be.android.library.worker.controllers.JobManager#JOB_GROUP_UNIQUE}
     * should not be enqueued when there are no any free threads.
     * In such case an extra executor should be allocated.
     * <br />
     * <br />
     * Any forked jobs with group {@link com.be.android.library.worker.controllers.JobManager#JOB_GROUP_UNIQUE}
     * has to have this flag to ensure correct execution flow.
     */
    public static final String FLAG_FORCE_EXECUTE = "com.be.android.library.worker.flags.FLAG_FORCE_EXECUTE";
    public static final String FLAG_JOB_ENQUEUED = "com.be.android.worker.params.FLAG_JOB_ENQUEUED";
    public static final String FLAG_JOB_SUBMITTED = "com.be.android.worker.params.FLAG_JOB_SUBMITTED";
    public static final String FLAG_JOB_PAUSED = "com.be.android.worker.params.FLAG_JOB_PAUSED";
    public static final String FLAG_USE_JOB_CREATOR = "com.be.android.worker.params.FLAG_USE_JOB_CREATOR";
    public static final String EXTRA_JOB_TYPE = "com.be.android.worker.params.EXTRA_JOB_TYPE";

    public static final List<String> PROTECTED_FLAGS = Arrays.asList(
            FLAG_JOB_ENQUEUED,
            FLAG_JOB_SUBMITTED,
            FLAG_JOB_PAUSED);

    JobParams copy();

    boolean isJobIdAssigned();

    void assignJobId(int jobId);

    int getJobId();

    Flags getFlags();

    String getJobClassName();

    int getGroupId();

    int getPriority();

    boolean hasPayload();

    Object getPayload();

    Collection<String> getTags();

    boolean hasTag(String tag);

    boolean hasTags(String... tags);

    boolean hasTags(Collection<String> tags);

    Map<String, Object> getExtras();

    Object getExtra(String key);

    <T> T getExtra(String key, T defaultValue);

    boolean hasExtra(String key);
}
