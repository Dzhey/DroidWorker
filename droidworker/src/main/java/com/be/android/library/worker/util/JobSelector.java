package com.be.android.library.worker.util;

import com.be.android.library.worker.base.JobStatus;
import com.be.android.library.worker.interfaces.Job;
import com.be.android.library.worker.models.Flags;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Used to match jobs
 */
public class JobSelector {

    private int[] mJobIds;
    private String[] mJobTags;
    private JobStatus[] mJobStatus;
    private Map<String, Boolean> mJobFlags;
    private Map<String, Object> mJobExtras;
    private boolean mIsAnyFlag;
    private boolean mIsAnyTag;

    public static JobSelector create() {
        return new JobSelector();
    }

    public static JobSelector forJobId(int... jobId) {
        return new JobSelector().jobId(jobId);
    }

    public static JobSelector forJobStatus(JobStatus... status) {
        return new JobSelector().jobStatus(status);
    }

    public static JobSelector forJobTags(String... tags) {
        return new JobSelector().tags(tags);
    }

    public static JobSelector forAnyJobTags(String... tags) {
        return new JobSelector().tags(tags).setIsAnyTag(true);
    }

    public static <T extends Collection<String>> JobSelector forJobTags(T tags) {
        return new JobSelector().tags(tags);
    }

    public static <T extends Collection<String>> JobSelector forAnyJobTags(T tags) {
        return new JobSelector().tags(tags).setIsAnyTag(true);
    }

    public static JobSelector forJobFlags(String... flags) {
        return new JobSelector().flags(flags);
    }

    public static <T extends Collection<String>> JobSelector forJobFlags(T flags) {
        return new JobSelector().flags(flags);
    }

    public static JobSelector forAnyJobFlags(String... flags) {
        return new JobSelector().flags(flags).setIsAnyFlag(true);
    }

    public static <T extends Collection<String>> JobSelector forAnyJobFlags(T flags) {
        return new JobSelector().flags(flags).setIsAnyFlag(true);
    }

    public static JobSelector forJobExtra(String name, Object value) {
        final Map<String, Object> map = new HashMap<String, Object>(1);
        map.put(name, value);

        return new JobSelector().extras(map);
    }

    public JobSelector() {
    }

    public boolean apply(Job job) {
        if (mJobIds != null
                && job.hasId()
                && Arrays.binarySearch(mJobIds, job.getJobId()) < 0) {

            return false;
        }

        if (mJobStatus != null) {
            final JobStatus jobStatus = job.getStatus();
            boolean hasStatus = false;
            for (JobStatus status : mJobStatus) {
                if (status == jobStatus) {
                    hasStatus = true;
                    break;
                }
            }

            if (hasStatus == false) {
                return false;
            }
        }

        return applyForTags(job, mJobTags)
                    && applyForFlags(job, getFlags())
                    && applyForExtras(job, getExtras());
    }

    public boolean isAnyTag() {
        return mIsAnyTag;
    }

    public JobSelector setIsAnyTag(boolean isAnyTag) {
        mIsAnyTag = isAnyTag;

        return this;
    }

    protected boolean applyForTags(Job job, String[] tags) {
        if (mIsAnyTag) {
            return applyForAnyTags(job, tags);
        }

        return tags == null || tags.length == 0 || job.getParams().hasTags(mJobTags);

    }

    protected boolean applyForAnyTags(Job job, String[] tags) {
        if (tags == null || tags.length == 0) {
            return true;
        }

        for (String tag : tags) {
            if (job.getParams().hasTag(tag)) {
                return true;
            }
        }

        return false;
    }

    protected <T extends Map<String, Boolean>> boolean applyForFlags(Job job, T flags) {
        if (mIsAnyFlag) {
            return applyForAnyFlags(job, flags);
        }

        final Flags jobFlags = job.getParams().getFlags();

        for (Map.Entry<String, Boolean> entry : flags.entrySet()) {
            final String flagName = entry.getKey();
            final Boolean flagValue = entry.getValue();

            if (!jobFlags.hasFlag(flagName)) {
                return false;
            }

            if (flagValue == null) {
                continue;
            }

            if (jobFlags.checkFlag(flagName) != flagValue) {
                return false;
            }
        }

        return true;
    }

    private boolean applyForAnyFlags(Job job, Map<String, Boolean> flags) {
        if (flags.isEmpty()) {
            return true;
        }

        final Flags jobFlags = job.getParams().getFlags();

        for (Map.Entry<String, Boolean> entry : flags.entrySet()) {
            final String flagName = entry.getKey();
            final Boolean flagValue = entry.getValue();

            if (!jobFlags.hasFlag(flagName)) {
                continue;
            }

            if (flagValue == null || jobFlags.checkFlag(flagName) == flagValue) {
                return true;
            }
        }

        return false;
    }

    protected <T, V extends Map<String, T>> boolean applyForExtras(Job job, V extras) {
        final Map<String, Object> jobExtras = job.getParams().getExtras();

        for (Map.Entry<String, T> entry : extras.entrySet()) {
            final String name = entry.getKey();
            final Object value = entry.getValue();

            if (!jobExtras.containsKey(name)) {
                return false;
            }

            final Object jobExtraValue = jobExtras.get(name);
            if (jobExtraValue == null) {
                if (value == null) {
                    continue;
                }

                return false;
            }

            if (!jobExtraValue.equals(value)) {
                return false;
            }
        }

        return true;
    }

    public Map<String, Boolean> getFlags() {
        return Collections.unmodifiableMap(getFlagsImpl());
    }

    protected Map<String, Boolean> getFlagsImpl() {
        if (mJobFlags == null) {
            mJobFlags = new HashMap<String, Boolean>();
        }

        return mJobFlags;
    }

    public Map<String, Object> getExtras() {
        return Collections.unmodifiableMap(getExtrasImpl());
    }

    protected Map<String, Object> getExtrasImpl() {
        if (mJobExtras == null) {
            mJobExtras = new HashMap<String, Object>();
        }

        return mJobExtras;
    }

    public JobSelector jobId(int... jobId) {
        mJobIds = jobId;

        return this;
    }

    public JobSelector addJobId(int jobId) {
        if (mJobIds == null) {
            mJobIds = new int[] { jobId };

        } else if (Arrays.binarySearch(mJobIds, jobId) < 0) {
            int[] jobIds = new int[mJobIds.length + 1];
            System.arraycopy(mJobIds, 0, jobIds, 0, mJobIds.length);
            mJobIds = jobIds;
            mJobIds[mJobIds.length - 1] = jobId;
        }

        return this;
    }

    public JobSelector removeJobId(int jobId) {
        if (mJobIds == null) {
            return this;
        }

        if (Arrays.binarySearch(mJobIds, jobId) < 0) {
            return this;
        }

        if (mJobIds.length == 1) {
            mJobIds = null;
            return this;
        }

        int[] ids = new int[mJobIds.length - 1];
        int counter = 0;
        for (int id : mJobIds) {
            if (id != jobId) {
                ids[counter] = id;
                counter++;
            }
        }
        mJobIds = ids;

        return this;
    }

    public JobSelector tags(String... tags) {
        mJobTags = tags;

        return this;
    }

    public <T extends Collection<String>> JobSelector tags(T tags) {
        mJobTags = tags.toArray(new String[tags.size()]);

        return this;
    }

    public JobSelector addTag(String tag) {
        if (tag == null) {
            throw new IllegalArgumentException("tag == null");
        }

        if (mJobTags == null) {
            mJobTags = new String[] {tag};

        } else if (Arrays.binarySearch(mJobTags, tag) < 0) {
            String[] tags = new String[mJobTags.length + 1];
            System.arraycopy(mJobTags, 0, tags, 0, mJobTags.length);
            mJobTags = tags;
            tags[mJobTags.length - 1] = tag;
        }

        return this;
    }

    public JobSelector removeTag(String tag) {
        if (tag == null) {
            throw new IllegalArgumentException("tag == null");
        }

        if (mJobTags == null) {
            return this;
        }

        if (Arrays.binarySearch(mJobTags, tag) < 0) {
            return this;
        }

        if (mJobTags.length == 1) {
            mJobTags = null;
            return this;
        }

        String[] tags = new String[mJobTags.length - 1];
        int counter = 0;
        for (String item : mJobTags) {
            if (item.equals(tag) == false) {
                tags[counter] = tag;
                counter++;
            }
        }
        mJobTags = tags;

        return this;
    }

    public JobSelector jobStatus(JobStatus... jobStatus) {
        mJobStatus = jobStatus;

        return this;
    }

    public JobSelector addJobStatus(JobStatus status) {
        if (mJobStatus == null) {
            mJobStatus = new JobStatus[] { status };

        } else if (Arrays.binarySearch(mJobStatus, status) < 0) {
            JobStatus[] jobStatus = new JobStatus[mJobStatus.length + 1];
            System.arraycopy(mJobStatus, 0, jobStatus, 0, mJobStatus.length);
            mJobStatus = jobStatus;
            mJobStatus[mJobStatus.length - 1] = status;
        }

        return this;
    }

    public JobSelector removeJobStatus(JobStatus status) {
        if (mJobStatus == null) {
            return this;
        }

        if (Arrays.binarySearch(mJobStatus, status) < 0) {
            return this;
        }

        if (mJobStatus.length == 1) {
            mJobStatus = null;
            return this;
        }

        JobStatus[] items = new JobStatus[mJobStatus.length - 1];
        int counter = 0;
        for (JobStatus item : mJobStatus) {
            if (item != status) {
                items[counter] = item;
                counter++;
            }
        }
        mJobStatus = items;

        return this;
    }

    public JobSelector flags(String... flags) {
        final Map<String, Boolean> myFlags = getFlagsImpl();
        myFlags.clear();

        for (String flag : flags) {
            myFlags.put(flag, null);
        }

        return this;
    }

    public <T extends Collection<String>> JobSelector flags(T flags) {
        final Map<String, Boolean> myFlags = getFlagsImpl();
        myFlags.clear();

        for (String flag : flags) {
            myFlags.put(flag, null);
        }

        return this;
    }

    public <T extends Map<String, Boolean>> JobSelector flags(T flags) {
        final Map<String, Boolean> myFlags = getFlagsImpl();
        myFlags.clear();
        myFlags.putAll(flags);

        return this;
    }

    public JobSelector addFlag(String flag) {
        getFlagsImpl().put(flag, null);

        return this;
    }

    public JobSelector addFlag(String flag, Boolean value) {
        getFlagsImpl().put(flag, value);

        return this;
    }

    public JobSelector removeFlag(String flag) {
        getFlagsImpl().remove(flag);

        return this;
    }

    public JobSelector removeFlags(String... flags) {
        final Map<String, Boolean> myFlags = getFlagsImpl();
        for (String flag : flags) {
            myFlags.remove(flag);
        }

        return this;
    }

    public JobSelector removeFlags(Collection<String> flags) {
        final Map<String, Boolean> myFlags = getFlagsImpl();
        for (String flag : flags) {
            myFlags.remove(flag);
        }

        return this;
    }

    public JobSelector setIsAnyFlag(boolean considerAnyFlag) {
        mIsAnyFlag = considerAnyFlag;

        return this;
    }

    public boolean getIsAnyFlag() {
        return mIsAnyFlag;
    }

    public JobSelector addExtra(String name, Object value) {
        final Map<String, Object> myExtras = getExtrasImpl();
        myExtras.put(name, value);

        return this;
    }

    public JobSelector removeExtra(String name) {
        final Map<String, Object> myExtras = getExtrasImpl();
        myExtras.remove(name);

        return this;
    }

    public <T, V extends Map<String, T>> JobSelector extras(V extras) {
        final Map<String, Object> myExtras = getExtrasImpl();
        myExtras.clear();
        myExtras.putAll(extras);

        return this;
    }
}
