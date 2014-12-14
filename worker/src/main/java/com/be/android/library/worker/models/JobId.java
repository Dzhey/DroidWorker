package com.be.android.library.worker.models;

import com.be.android.library.worker.interfaces.Job;

import java.util.List;

public class JobId {
    private int jobId;
    private int jobGroupId;
    private int jobPriority;
    private String jobTypeName;
    private String jobTypeNameShort;
    private List<String> jobTags;

    public static JobId of(Job job) {
        return new JobId(job);
    }

    private JobId(Job job) {
        this.jobId = job.getJobId();
        this.jobGroupId = job.getGroupId();
        this.jobPriority = job.getPriority();
        this.jobTags = job.getTags();
        this.jobTypeName = job.getClass().getName();
        this.jobTypeNameShort = job.getClass().getSimpleName();
    }

    public int getJobId() {
        return jobId;
    }

    public int getJobGroupId() {
        return jobGroupId;
    }

    public int getJobPriority() {
        return jobPriority;
    }

    public String getJobTypeName() {
        return jobTypeName;
    }

    public List<String> getJobTags() {
        return jobTags;
    }

    @Override
    public String toString() {
        return "JobId{" +
                "jobId=" + jobId +
                ", jobGroupId=" + jobGroupId +
                ", jobPriority=" + jobPriority +
                ", jobTypeNameShort='" + jobTypeNameShort + '\'' +
                ", jobTags=" + jobTags +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JobId jobId1 = (JobId) o;

        if (jobGroupId != jobId1.jobGroupId) return false;
        if (jobId != jobId1.jobId) return false;
        if (jobPriority != jobId1.jobPriority) return false;
        if (!jobTypeName.equals(jobId1.jobTypeName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = jobId;
        result = 31 * result + jobGroupId;
        result = 31 * result + jobPriority;
        result = 31 * result + jobTypeName.hashCode();
        return result;
    }
}
