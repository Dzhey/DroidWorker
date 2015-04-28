package com.be.android.library.worker.util;

import com.be.android.library.worker.base.JobStatus;
import com.be.android.library.worker.interfaces.Job;

import java.util.Arrays;
import java.util.Collection;

/**
 * Used to match jobs
 */
public class JobSelector {

    private int[] mJobIds;
    private String[] mJobTags;
    private JobStatus[] mJobStatus;

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

    public static JobSelector forJobTags(Collection<String> tags) {
        return new JobSelector().tags(tags);
    }

    JobSelector() {
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

        if (mJobTags != null) {
            if (!job.getParams().hasTags(mJobTags)) {
                return false;
            }
        }

        return true;
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

    public JobSelector tags(Collection<String> tags) {
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
}
