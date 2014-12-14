package com.be.android.library.worker.base;

import com.be.android.library.worker.controllers.JobManager;
import com.be.android.library.worker.interfaces.Job;
import com.be.android.library.worker.models.JobResultStatus;

import java.util.Collections;
import java.util.List;

public class JobEvent {

    public static final int RESULT_CODE_UNSPECIFIED = -1;
    public static final int RESULT_CODE_OK = 0;
    public static final int RESULT_CODE_FAILED = 1;
    public static final int RESULT_CODE_CANCELLED = 2;

    public static final int UPDATE_CODE_STATUS_CHANGED = 1000;
    public static final int UPDATE_CODE_PROGRESS_UPDATE = 1001;
    public static final int UPDATE_CODE_STATUS_MESSAGE_CHANGED = 1002;

    private int eventCode = RESULT_CODE_UNSPECIFIED;
    private int jobId = JobManager.JOB_ID_UNSPECIFIED;
    private int jobGroupId = JobManager.JOB_GROUP_DEFAULT;
    private JobStatus jobStatus;
    private boolean isJobFinished;
    private Exception uncaughtException;
    private String extraMessage;
    private List<String> jobTags;
    private Job job;

    public static JobEvent fromEvent(JobEvent e) {
        return new JobEvent(e);
    }

    public static JobEvent ok() {
        return new JobEvent(RESULT_CODE_OK, JobStatus.OK);
    }

    public static JobEvent ok(String extraMessage) {
        JobEvent result = new JobEvent(RESULT_CODE_OK, JobStatus.OK);

        result.setExtraMessage(extraMessage);

        return result;
    }

    public static JobEvent failure() {
        return new JobEvent(RESULT_CODE_FAILED, JobStatus.FAILED);
    }

    public static JobEvent failure(String extraMessage, Exception exception) {
        JobEvent result = new JobEvent(RESULT_CODE_FAILED, JobStatus.FAILED);

        result.setUncaughtException(exception);
        result.setExtraMessage(extraMessage);

        return result;
    }

    public JobEvent(JobResultStatus status) {
        this(JobManager.JOB_ID_UNSPECIFIED,
                JobManager.JOB_GROUP_DEFAULT,
                JobResultStatus.getResultCode(status),
                null,
                JobStatus.fromJobResultStatus(status),
                null,
                null);
    }

    protected JobEvent(int eventCode) {
        this(JobManager.JOB_ID_UNSPECIFIED,
                JobManager.JOB_GROUP_DEFAULT,
                eventCode,
                null,
                null,
                null,
                null);
    }

    protected JobEvent(int eventCode, JobResultStatus status) {
        this(JobManager.JOB_ID_UNSPECIFIED,
                JobManager.JOB_GROUP_DEFAULT,
                eventCode,
                null,
                JobStatus.fromJobResultStatus(status),
                null,
                null);
    }

    protected JobEvent(int eventCode, JobStatus status) {
        this(JobManager.JOB_ID_UNSPECIFIED,
                JobManager.JOB_GROUP_DEFAULT,
                eventCode,
                null,
                status,
                null,
                null);
    }

    protected JobEvent(JobEvent other) {
        setJobGroupId(other.getJobGroupId());
        setEventCode(other.getEventCode());
        setJobTags(other.getJobTags());
        setJobStatus(other.getJobStatus());
        setJob(other.getJob());
        setUncaughtException(other.getUncaughtException());
        setExtraMessage(other.getExtraMessage());
        setJobFinished(other.isJobFinished());
    }

    public JobEvent(int jobId, int groupId, int eventCode, List<String> jobTags,
                    JobStatus jobStatus, Job job, Exception uncaughtException) {

        this.jobId = jobId;
        this.jobGroupId = groupId;
        this.eventCode = eventCode;
        this.jobStatus = jobStatus;
        if (jobTags == null) {
            this.jobTags = Collections.emptyList();
        } else {
            this.jobTags = jobTags;
        }

        this.uncaughtException = null;
        this.job = job;
    }

    public int getJobId() {
        return jobId;
    }

    void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public int getJobGroupId() {
        return jobGroupId;
    }

    void setJobGroupId(int jobGroupId) {
        this.jobGroupId = jobGroupId;
    }

    public int getEventCode() {
        return eventCode;
    }

    public JobEvent setEventCode(int eventCode) {
        this.eventCode = eventCode;

        return this;
    }

    public JobStatus getJobStatus() {
        return jobStatus;
    }

    void setJobStatus(JobStatus jobStatus) {
        this.jobStatus = jobStatus;
    }

    public List<String> getJobTags() {
        return jobTags;
    }

    void setJobTags(List<String> jobTags) {
        this.jobTags = jobTags;
    }

    public boolean isJobIdAssigned() {
        return jobId != JobManager.JOB_ID_UNSPECIFIED;
    }

    public Exception getUncaughtException() {
        return uncaughtException;
    }

    JobEvent setUncaughtException(Exception e) {
        this.uncaughtException = e;

        return this;
    }

    public String getExtraMessage() {
        return extraMessage;
    }

    public JobEvent setExtraMessage(String extraMessage) {
        this.extraMessage = extraMessage;

        return this;
    }

    public Job getJob() {
        return job;
    }

    void setJob(Job job) {
        this.job = job;
    }

    public boolean isJobFinished() {
        return isJobFinished;
    }

    void setJobFinished(boolean isJobFinished) {
        this.isJobFinished = isJobFinished;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                "{" +
                "eventCode=" + eventCode +
                ", jobId=" + jobId +
                ", jobGroupId=" + jobGroupId +
                ", jobStatus=" + jobStatus +
                ", uncaughtException=" + uncaughtException +
                ", extraMessage='" + extraMessage + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JobEvent jobEvent = (JobEvent) o;

        if (jobGroupId != jobEvent.jobGroupId) return false;
        if (jobId != jobEvent.jobId) return false;
        if (eventCode != jobEvent.eventCode) return false;
        if (jobStatus != jobEvent.jobStatus) return false;
        if (uncaughtException != null ? !uncaughtException.equals(jobEvent.uncaughtException) : jobEvent.uncaughtException != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = eventCode;
        result = 31 * result + jobId;
        result = 31 * result + jobGroupId;
        result = 31 * result + jobStatus.hashCode();
        result = 31 * result + (uncaughtException != null ? uncaughtException.hashCode() : 0);
        return result;
    }
}
