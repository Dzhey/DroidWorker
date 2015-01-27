package com.be.android.library.worker.base;

import com.be.android.library.worker.controllers.JobManager;
import com.be.android.library.worker.interfaces.Job;
import com.be.android.library.worker.models.JobResultStatus;
import com.be.android.library.worker.util.EventCreator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class JobEvent {

    public static class Builder {

        private JobEvent mEvent;
        private boolean mIsFinished;

        public Builder() {
            mEvent = new JobEvent();
        }

        public Builder(JobEvent fromEvent) {
            mEvent = new JobEvent(fromEvent);
        }

        private void throwIfFinished() {
            if (mIsFinished) {
                throw new IllegalStateException("event is already built");
            }
        }

        protected void setEvent(JobEvent event) {
            mEvent = event;
        }

        protected JobEvent getEvent() {
            return mEvent;
        }

        public Builder jobId(int jobId) {
            throwIfFinished();

            mEvent.setJobId(jobId);

            return this;
        }

        public Builder eventCode(int eventCode) {
            throwIfFinished();

            mEvent.setEventCode(eventCode);

            return this;
        }

        public Builder extraCode(int extraCode) {
            throwIfFinished();

            mEvent.setExtraCode(extraCode);

            return this;
        }

        public Builder jobStatus(JobResultStatus jobResultStatus) {
            throwIfFinished();

            mEvent.setJobStatus(JobStatus.fromJobResultStatus(jobResultStatus));

            return this;
        }

        public Builder jobStatus(JobStatus jobStatus) {
            throwIfFinished();

            mEvent.setJobStatus(jobStatus);

            return this;
        }

        public Builder groupId(int jobGroupId) {
            throwIfFinished();

            mEvent.setJobGroupId(jobGroupId);

            return this;
        }

        public Builder setJobFinished(boolean isFinished) {
            throwIfFinished();

            mEvent.setJobFinished(isFinished);

            return this;
        }

        public Builder setJobFinished() {
            throwIfFinished();

            return setJobFinished(true);
        }

        public Builder job(Job job) {
            throwIfFinished();

            mEvent.setJob(job);

            return this;
        }

        public Builder uncaughtException(Exception uncaughtException) {
            throwIfFinished();

            mEvent.setUncaughtException(uncaughtException);

            return this;
        }

        public Builder extraMessage(String extraMessage) {
            throwIfFinished();

            mEvent.setExtraMessage(extraMessage);

            return this;
        }

        public Builder addTag(String tag) {
            throwIfFinished();

            if (tag == null) {
                throw new IllegalArgumentException("tag == null");
            }

            if (mEvent.mJobTags == null) {
                mEvent.mJobTags = new ArrayList<String>();
            }

            mEvent.mJobTags.add(tag);

            return this;
        }

        public Builder removeTag(String tag) {
            throwIfFinished();

            if (tag == null) {
                throw new IllegalArgumentException("tag == null");
            }

            if (mEvent.mJobTags != null) {
                mEvent.mJobTags.remove(tag);
            }

            return this;
        }

        public Builder tags(List<String> tags) {
            throwIfFinished();

            mEvent.setJobTags(tags);

            return this;
        }

        public JobEvent build() {
            throwIfFinished();

            mIsFinished = true;

            return mEvent;
        }
    }

    public static class EventBuilder<T extends JobEvent> extends Builder {

        public EventBuilder(EventCreator<T> eventCreator) {
            setEvent(eventCreator.createInstance());
        }

        public EventBuilder(JobEvent fromEvent, EventCreator<T> eventCreator) {
            JobEvent event = eventCreator.createInstance();
            event.copyFrom(fromEvent);
            setEvent(event);
        }

        @Override
        public EventBuilder<T> jobId(int jobId) {
            super.jobId(jobId);

            return this;
        }

        @Override
        public EventBuilder<T> eventCode(int eventCode) {
            super.eventCode(eventCode);

            return this;
        }

        @Override
        public EventBuilder<T> extraCode(int extraCode) {
            super.extraCode(extraCode);

            return this;
        }

        @Override
        public EventBuilder<T> jobStatus(JobResultStatus jobResultStatus) {
            super.jobStatus(jobResultStatus);

            return this;
        }

        @Override
        public EventBuilder<T> jobStatus(JobStatus jobStatus) {
            super.jobStatus(jobStatus);

            return this;
        }

        @Override
        public EventBuilder<T> groupId(int jobGroupId) {
            super.groupId(jobGroupId);

            return this;
        }

        @Override
        public EventBuilder<T> setJobFinished(boolean isFinished) {
            super.setJobFinished(isFinished);

            return this;
        }

        @Override
        public EventBuilder<T> setJobFinished() {
            super.setJobFinished();

            return this;
        }

        @Override
        public EventBuilder<T> job(Job job) {
            super.job(job);

            return this;
        }

        @Override
        public EventBuilder<T> uncaughtException(Exception uncaughtException) {
            super.uncaughtException(uncaughtException);

            return this;
        }

        @Override
        public EventBuilder<T> extraMessage(String extraMessage) {
            super.extraMessage(extraMessage);

            return this;
        }

        @Override
        public EventBuilder<T> addTag(String tag) {
            super.addTag(tag);

            return this;
        }

        @Override
        public EventBuilder<T> removeTag(String tag) {
            super.removeTag(tag);

            return this;
        }

        @Override
        public EventBuilder<T> tags(List<String> tags) {
            super.tags(tags);

            return this;
        }

        @Override
        public T build() {
            return (T) getEvent();
        }
    }

    public static final int EVENT_CODE_UNSPECIFIED = -1;
    public static final int EVENT_CODE_OK = 0;
    public static final int EVENT_CODE_FAILED = 1;
    public static final int EVENT_CODE_CANCELLED = 2;
    public static final int EVENT_CODE_UPDATE = 3;

    public static final int EXTRA_CODE_UNSPECIFIED = -1;
    public static final int EXTRA_CODE_STATUS_CHANGED = 0;
    public static final int EXTRA_CODE_PROGRESS_UPDATE = 1;
    public static final int EXTRA_CODE_STATUS_MESSAGE_CHANGED = 2;

    private int mJobId = JobManager.JOB_ID_UNSPECIFIED;
    private int mJobGroupId = JobManager.JOB_GROUP_DEFAULT;
    private int mEventCode = EVENT_CODE_UNSPECIFIED;
    private int mExtraCode = EXTRA_CODE_UNSPECIFIED;
    private JobStatus mJobStatus;
    private boolean mIsJobFinished;
    private Exception mUncaughtException;
    private String mExtraMessage;
    private List<String> mJobTags;
    private Job mJob;

    public static JobEvent fromEvent(JobEvent e) {
        return new JobEvent(e);
    }

    public static JobEvent ok() {
        JobEvent result = new JobEvent();

        result.setEventCode(EVENT_CODE_OK);
        result.setJobStatus(JobStatus.OK);

        return result;
    }

    public static JobEvent ok(String extraMessage) {
        JobEvent result = new JobEvent();

        result.setEventCode(EVENT_CODE_OK);
        result.setJobStatus(JobStatus.OK);
        result.setExtraMessage(extraMessage);

        return result;
    }

    public static JobEvent failure() {
        JobEvent result = new JobEvent();

        result.setEventCode(EVENT_CODE_FAILED);
        result.setJobStatus(JobStatus.FAILED);

        return result;
    }

    public static JobEvent failure(String extraMessage) {
        JobEvent result = new JobEvent();

        result.setEventCode(EVENT_CODE_FAILED);
        result.setJobStatus(JobStatus.FAILED);
        result.setExtraMessage(extraMessage);

        return result;
    }

    public static JobEvent failure(int eventCode, String extraMessage) {
        JobEvent result = new JobEvent();

        result.setEventCode(eventCode);
        result.setJobStatus(JobStatus.FAILED);
        result.setExtraMessage(extraMessage);

        return result;
    }

    public static JobEvent failure(String extraMessage, Exception exception) {
        JobEvent result = new JobEvent();

        result.setEventCode(EVENT_CODE_FAILED);
        result.setJobStatus(JobStatus.FAILED);
        result.setUncaughtException(exception);
        result.setExtraMessage(extraMessage);

        return result;
    }

    public static JobEvent failure(int eventCode, String extraMessage, Exception exception) {
        JobEvent result = new JobEvent();

        result.setEventCode(eventCode);
        result.setJobStatus(JobStatus.FAILED);
        result.setUncaughtException(exception);
        result.setExtraMessage(extraMessage);

        return result;
    }

    JobEvent(int eventCode) {
        mEventCode = eventCode;
    }

    JobEvent(int eventCode, int extraCode, JobStatus status) {
        mEventCode = eventCode;
        mExtraCode = extraCode;
        mJobStatus = status;
    }

    JobEvent(int eventCode, JobStatus status) {
        mEventCode = eventCode;
        mJobStatus = status;
    }

    protected JobEvent(JobEvent other) {
        copyFrom(other);
    }

    protected JobEvent() {
    }

    protected void copyFrom(JobEvent other) {
        mJobId = other.mJobId;
        mJobGroupId = other.mJobGroupId;
        mEventCode = other.mEventCode;
        mExtraCode = other.mExtraCode;
        mJobTags = other.mJobTags;
        mJobStatus = other.mJobStatus;
        mJob = other.mJob;
        mUncaughtException = other.mUncaughtException;
        mExtraMessage = other.mExtraMessage;
        mIsJobFinished = other.mIsJobFinished;
    }

    public int getJobId() {
        return mJobId;
    }

    protected void setJobId(int jobId) {
        mJobId = jobId;
    }

    public int getJobGroupId() {
        return mJobGroupId;
    }

    protected void setJobGroupId(int jobGroupId) {
        mJobGroupId = jobGroupId;
    }

    public int getEventCode() {
        return mEventCode;
    }

    protected void setEventCode(int eventCode) {
        mEventCode = eventCode;
    }

    public int getExtraCode() {
        return mExtraCode;
    }

    protected void setExtraCode(int extraCode) {
        mExtraCode = extraCode;
    }

    public JobStatus getJobStatus() {
        return mJobStatus;
    }

    protected void setJobStatus(JobStatus jobStatus) {
        mJobStatus = jobStatus;
    }

    protected void setJobStatus(JobResultStatus jobStatus) {
        mJobStatus = JobStatus.fromJobResultStatus(jobStatus);
    }

    public List<String> getJobTags() {
        if (mJobTags == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(mJobTags);
    }

    protected void setJobTags(Collection<String> jobTags) {
        if (jobTags == null) {
            mJobTags = null;
            return;
        }

        if (mJobTags == null) {
            mJobTags = new ArrayList<String>();
        } else {
            mJobTags.clear();
        }

        mJobTags.addAll(jobTags);
    }

    public boolean isJobIdAssigned() {
        return mJobId != JobManager.JOB_ID_UNSPECIFIED;
    }

    public boolean isEventCodeSpecified() {
        return mEventCode != EVENT_CODE_UNSPECIFIED;
    }

    public boolean isExtraCodeSpecified() {
        return mExtraCode != EXTRA_CODE_UNSPECIFIED;
    }

    public Exception getUncaughtException() {
        return mUncaughtException;
    }

    protected JobEvent setUncaughtException(Exception e) {
        this.mUncaughtException = e;

        return this;
    }

    public String getExtraMessage() {
        return mExtraMessage;
    }

    protected JobEvent setExtraMessage(String extraMessage) {
        this.mExtraMessage = extraMessage;

        return this;
    }

    public Job getJob() {
        return mJob;
    }

    protected void setJob(Job job) {
        this.mJob = job;
    }

    public boolean isJobFinished() {
        return mIsJobFinished;
    }

    protected void setJobFinished(boolean isJobFinished) {
        this.mIsJobFinished = isJobFinished;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                "{" +
                "mEventCode=" + mEventCode +
                "mExtraCode=" + mExtraCode +
                ", mJobId=" + mJobId +
                ", mJobGroupId=" + mJobGroupId +
                ", mJobStatus=" + mJobStatus +
                ", mUncaughtException=" + mUncaughtException +
                ", mExtraMessage='" + mExtraMessage + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JobEvent jobEvent = (JobEvent) o;

        if (mJobGroupId != jobEvent.mJobGroupId) return false;
        if (mJobId != jobEvent.mJobId) return false;
        if (mEventCode != jobEvent.mEventCode) return false;
        if (mExtraCode != jobEvent.mExtraCode) return false;
        if (mJobStatus != jobEvent.mJobStatus) return false;
        if (mUncaughtException != null ? !mUncaughtException.equals(jobEvent.mUncaughtException) : jobEvent.mUncaughtException != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = mEventCode;
        result = 31 * result + mJobId;
        result = 31 * result + mExtraCode;
        result = 31 * result + mJobGroupId;
        result = 31 * result + mJobStatus.hashCode();
        result = 31 * result + (mUncaughtException != null ? mUncaughtException.hashCode() : 0);
        return result;
    }
}
