package com.be.android.library.worker.util;

import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.base.JobStatus;
import com.be.android.library.worker.interfaces.Job;

import java.util.List;

public class JobEventFilter {

    public static class Builder {
        private JobEventFilter mJobEventFilter;
        private boolean mIsFinished;

        public Builder() {
            mJobEventFilter = new JobEventFilter();
        }

        public Builder pendingStatus(JobStatus... status) {
            throwIfFinished();

            mJobEventFilter.mPendingStatus = status;

            return this;
        }

        public Builder pendingEventCode(int... eventCode) {
            throwIfFinished();

            mJobEventFilter.mPendingEventCode = eventCode;

            return this;
        }

        public Builder pendingExtraCode(int... extraCode) {
            throwIfFinished();

            mJobEventFilter.mPendingExtraCode = extraCode;

            return this;
        }

        public Builder pendingTags(String... tags) {
            throwIfFinished();

            mJobEventFilter.mPendingTags = tags;

            return this;
        }

        public Builder pendingJobType(Class<? extends Job> jobType) {
            throwIfFinished();

            mJobEventFilter.mPendingJobType = jobType;

            return this;
        }

        public JobEventFilter build() {
            throwIfFinished();

            mIsFinished = true;

            return mJobEventFilter;
        }

        private void throwIfFinished() {
            if (mIsFinished) {
                throw new IllegalStateException("event is already built");
            }
        }
    }

    private JobStatus[] mPendingStatus;
    private int[] mPendingEventCode;
    private int[] mPendingExtraCode;
    private String[] mPendingTags;
    private Class<?> mPendingJobType;

    protected JobEventFilter() {
    }

    public boolean apply(JobEvent event) {
        return checkPendingStatus(event)
                && checkPendingJobType(event)
                && checkPendingEventCode(event)
                && checkPendingExtraCode(event)
                && checkPendingTags(event);
    }

    protected boolean checkPendingJobType(JobEvent event) {
        Class<?> pendingJobType = mPendingJobType;

        if (pendingJobType == null || pendingJobType.equals(Job.class)) {
            return true;
        }

        return pendingJobType.equals(event.getJob().getClass());
    }

    protected boolean checkPendingStatus(JobEvent event) {
        if (mPendingStatus == null || mPendingStatus.length == 0) {
            return true;
        }

        for (JobStatus status : mPendingStatus) {
            if (status == event.getJobStatus()) {
                return true;
            }
        }

        return false;
    }

    protected boolean checkPendingEventCode(JobEvent event) {
        if (mPendingEventCode == null || mPendingEventCode.length == 0) {
            return true;
        }

        for (int eventCode : mPendingEventCode) {
            if (eventCode == event.getEventCode()) {
                return true;
            }
        }

        return false;
    }

    protected boolean checkPendingExtraCode(JobEvent event) {
        if (mPendingExtraCode == null || mPendingExtraCode.length == 0) {
            return true;
        }

        for (int extraCode : mPendingExtraCode) {
            if (extraCode == event.getExtraCode()) {
                return true;
            }
        }

        return false;
    }

    protected boolean checkPendingTags(JobEvent event) {
        if (mPendingTags == null || mPendingTags.length == 0) {
            return true;
        }

        final List<String> jobTags = event.getJobTags();

        for (String pendingTag : mPendingTags) {
            if (jobTags.contains(pendingTag) == false) {
                return false;
            }
        }

        return true;
    }
}
