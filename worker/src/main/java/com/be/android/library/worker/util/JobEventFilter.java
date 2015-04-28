package com.be.android.library.worker.util;

import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.base.JobStatus;
import com.be.android.library.worker.interfaces.FlagsProvider;
import com.be.android.library.worker.interfaces.Job;
import com.be.android.library.worker.models.Flag;
import com.be.android.library.worker.models.JobParams;

import java.util.Collection;
import java.util.List;

public class JobEventFilter {

    public static class Builder {
        private JobEventFilter mJobEventFilter;
        private boolean mIsBuilt;

        public Builder() {
            mJobEventFilter = new JobEventFilter();
        }

        public Builder pendingStatus(JobStatus... status) {
            throwIfBuilt();

            mJobEventFilter.mPendingStatus = status;

            return this;
        }

        public Builder pendingEventCode(int... eventCode) {
            throwIfBuilt();

            mJobEventFilter.mPendingEventCode = eventCode;

            return this;
        }

        public Builder pendingExtraCode(int... extraCode) {
            throwIfBuilt();

            mJobEventFilter.mPendingExtraCode = extraCode;

            return this;
        }

        public Builder pendingTags(String... tags) {
            throwIfBuilt();

            mJobEventFilter.mPendingTags = tags;

            return this;
        }

        public Builder pendingFlags(Flag... flags) {
            throwIfBuilt();

            mJobEventFilter.mPendingFlags = flags;

            return this;
        }

        public Builder pendingFlags(Collection<Flag> flags) {
            throwIfBuilt();

            mJobEventFilter.mPendingFlags = flags.toArray(new Flag[flags.size()]);

            return this;
        }

        public Builder pendingJobType(Class<? extends Job> jobType) {
            throwIfBuilt();

            mJobEventFilter.mPendingJobType = jobType;

            return this;
        }

        public JobEventFilter build() {
            throwIfBuilt();

            mIsBuilt = true;

            return mJobEventFilter;
        }

        private void throwIfBuilt() {
            if (mIsBuilt) {
                throw new IllegalStateException("event is already built");
            }
        }
    }

    private JobStatus[] mPendingStatus;
    private Flag[] mPendingFlags;
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
                && checkPendingTags(event)
                && checkPendingFlags(event);
    }

    protected boolean checkPendingJobType(JobEvent event) {
        Class<?> pendingJobType = mPendingJobType;

        if (pendingJobType == null || pendingJobType.equals(Job.class)) {
            return true;
        }

        return pendingJobType.getName().equals(event.getJobParams().getJobClassName());
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

        if (event.getJobParams().hasTags(mPendingTags) == false) {
            return false;
        }

        return true;
    }

    protected boolean checkPendingFlags(JobEvent event) {
        if (mPendingFlags == null || mPendingFlags.length == 0) {
            return true;
        }

        final FlagsProvider flags = event.getJobParams().getFlags();
        for (Flag pendingFlag : mPendingFlags) {
            final String name = pendingFlag.getName();

            if (!flags.hasFlag(name)) {
                return false;
            }

            if (!flags.checkFlag(name) == pendingFlag.getValue()) {
                return false;
            }
        }

        return true;
    }
}
