package com.be.android.library.worker.base;

import com.be.android.library.worker.interfaces.FlagsProvider;
import com.be.android.library.worker.models.Flags;
import com.be.android.library.worker.models.JobParams;
import com.be.android.library.worker.models.JobResultStatus;
import com.be.android.library.worker.util.EventCreator;

public class JobEvent implements FlagsProvider {

    public static class Builder {

        private JobEvent mEvent;
        private boolean mIsBuilt;

        public Builder() {
            mEvent = new JobEvent();
        }

        public Builder(JobEvent fromEvent) {
            mEvent = new JobEvent(fromEvent);
        }

        private void throwIfBuilt() {
            if (mIsBuilt) {
                throw new IllegalStateException("event is already built");
            }
        }

        protected void setEvent(JobEvent event) {
            mEvent = event;
        }

        protected JobEvent getEvent() {
            return mEvent;
        }

        public Builder params(JobParams params) {
            throwIfBuilt();

            mEvent.mJobParams = params;

            return this;
        }

        public Builder eventCode(int eventCode) {
            throwIfBuilt();

            mEvent.setEventCode(eventCode);

            return this;
        }

        public Builder extraCode(int extraCode) {
            throwIfBuilt();

            mEvent.setExtraCode(extraCode);

            return this;
        }

        public Builder jobStatus(JobResultStatus jobResultStatus) {
            throwIfBuilt();

            mEvent.setJobStatus(JobStatus.fromJobResultStatus(jobResultStatus));

            return this;
        }

        public Builder jobStatus(JobStatus jobStatus) {
            throwIfBuilt();

            mEvent.setJobStatus(jobStatus);

            return this;
        }

        public Builder uncaughtException(Exception uncaughtException) {
            throwIfBuilt();

            mEvent.setUncaughtException(uncaughtException);

            return this;
        }

        public Builder extraMessage(String extraMessage) {
            throwIfBuilt();

            mEvent.setExtraMessage(extraMessage);

            return this;
        }

        public Builder payload(Object payload) {
            throwIfBuilt();

            mEvent.setPayload(payload);

            return this;
        }

        public Builder flag(String flag, boolean value) {
            throwIfBuilt();

            mEvent.setFlag(flag, value);

            return this;
        }

        public Builder flag(String flag) {
            throwIfBuilt();

            mEvent.setFlag(flag, true);

            return this;
        }

        public JobEvent build() {
            throwIfBuilt();

            if (mEvent.mJobStatus == null) {
                throw new IllegalStateException("no job status defined");
            }

            mIsBuilt = true;

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
        public EventBuilder<T> params(JobParams params) {
            super.params(params);

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
        public EventBuilder<T> payload(Object payload) {
            super.payload(payload);

            return this;
        }

        @Override
        public Builder flag(String flag, boolean value) {
            super.flag(flag, value);

            return this;
        }

        @Override
        public Builder flag(String flag) {
            super.flag(flag);

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
    public static final int EXTRA_CODE_STATUS_CHANGED = 1;
    public static final int EXTRA_CODE_PROGRESS_UPDATE = 2;
    public static final int EXTRA_CODE_STATUS_MESSAGE_CHANGED = 3;
    public static final int EXTRA_CODE_FLAG_STATUS_CHANGED = 4;

    private int mEventCode = EVENT_CODE_UNSPECIFIED;
    private int mExtraCode = EXTRA_CODE_UNSPECIFIED;
    private Flags mFlags = new Flags();
    private JobStatus mJobStatus;
    private Exception mUncaughtException;
    private String mExtraMessage;
    private JobParams mJobParams;
    private Object mPayload;

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

    protected JobEvent(int eventCode) {
        mEventCode = eventCode;
    }

    protected JobEvent(int eventCode, int extraCode, JobStatus status) {
        mEventCode = eventCode;
        mExtraCode = extraCode;
        mJobStatus = status;
    }

    protected JobEvent(int eventCode, JobStatus status) {
        mEventCode = eventCode;
        mJobStatus = status;
    }

    protected JobEvent(JobEvent other) {
        copyFrom(other);
    }

    protected JobEvent() {
    }

    public int getJobId() {
        if (mJobParams == null) {
            throw new IllegalStateException("no job params defined");
        }

        return mJobParams.getJobId();
    }

    public boolean isJobIdAssigned() {
        return mJobParams != null && mJobParams.isJobIdAssigned();
    }

    public boolean hasParams() {
        return mJobParams != null;
    }

    public boolean isJobFinished() {
        return mJobStatus == JobStatus.OK
                || mJobStatus == JobStatus.FAILED
                || mJobStatus == JobStatus.CANCELLED;
    }

    protected final void copyFrom(JobEvent other) {
        mEventCode = other.mEventCode;
        mExtraCode = other.mExtraCode;
        mJobStatus = other.mJobStatus;
        mUncaughtException = other.mUncaughtException;
        mExtraMessage = other.mExtraMessage;
        mJobParams = other.mJobParams;
    }

    public JobParams getJobParams() {
        return mJobParams;
    }

    protected void setJobParams(JobParams jobParams) {
        mJobParams = jobParams;
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

    public Object getPayload() {
        return mPayload;
    }

    protected void setPayload(Object payload) {
        mPayload = payload;
    }

    public Flags getFlags() {
        return mFlags;
    }

    @Override
    public boolean checkFlag(String flag) {
        return mFlags.checkFlag(flag);
    }

    @Override
    public boolean hasFlag(String flag) {
        return mFlags.hasFlag(flag);
    }

    @Override
    public void setFlag(String flag, boolean value) {
        mFlags.setFlag(flag, value);
    }

    @Override
    public void setFlag(String flag) {
        mFlags.setFlag(flag);
    }

    @Override
    public void removeFlag(String flag) {
        mFlags.removeFlag(flag);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JobEvent jobEvent = (JobEvent) o;

        if (mEventCode != jobEvent.mEventCode) return false;
        if (mExtraCode != jobEvent.mExtraCode) return false;
        if (mExtraMessage != null ? !mExtraMessage.equals(jobEvent.mExtraMessage) : jobEvent.mExtraMessage != null)
            return false;
        if (mJobParams != null ? !mJobParams.equals(jobEvent.mJobParams) : jobEvent.mJobParams != null)
            return false;
        if (mJobStatus != jobEvent.mJobStatus) return false;
        if (mUncaughtException != null ? !mUncaughtException.equals(jobEvent.mUncaughtException) : jobEvent.mUncaughtException != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = mEventCode;
        result = 31 * result + mExtraCode;
        result = 31 * result + (mJobStatus != null ? mJobStatus.hashCode() : 0);
        result = 31 * result + (mUncaughtException != null ? mUncaughtException.hashCode() : 0);
        result = 31 * result + (mExtraMessage != null ? mExtraMessage.hashCode() : 0);
        result = 31 * result + (mJobParams != null ? mJobParams.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "JobEvent{" +
                "mEventCode=" + mEventCode +
                ", mExtraCode=" + mExtraCode +
                ", mJobStatus=" + mJobStatus +
                ", mUncaughtException=" + mUncaughtException +
                ", mExtraMessage='" + mExtraMessage + '\'' +
                ", mJobParams=" + mJobParams +
                '}';
    }
}
