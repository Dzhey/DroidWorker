package com.be.android.library.worker.base;

import android.util.Log;

import com.be.android.library.worker.controllers.JobEventObservableImpl;
import com.be.android.library.worker.controllers.JobManager;
import com.be.android.library.worker.exceptions.JobExecutionException;
import com.be.android.library.worker.interfaces.JobEventObservable;
import com.be.android.library.worker.util.JobFutureResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class BaseJob extends JobObservable {

    protected interface ExecutionHandler {
        public void onPreExecute();
        public JobEvent execute();
        public void onPostExecute(JobEvent executionResult);
        public void onExceptionCaughtBase(Exception e);
        public JobEvent executeImpl() throws Exception;
    }

    public static final String LOG_TAG = BaseJob.class.getSimpleName();

    private int mId = JobManager.JOB_ID_UNSPECIFIED;
    private int mGroupId = JobManager.JOB_GROUP_DEFAULT;
    private int mPriority;
    private JobStatus mStatus = JobStatus.PENDING;
    private Object mPayload;
    private List<String> mTags;
    private ReadWriteLock mTagsLock;
    private boolean mIsCancelled;
    private volatile boolean mIsFinished;
    private Boolean mIsPaused;
    private CountDownLatch mPauseLatch;
    private Lock mPauseLock;
    private ExecutionHandler mExecutionHandler;

    protected BaseJob() {
        this(new JobEventObservableImpl());
    }

    protected BaseJob(String... tags) {
        this(new JobEventObservableImpl(), tags);
    }

    protected BaseJob(JobEventObservable jobObservableHelper) {
        this(jobObservableHelper, (String[]) null);
    }

    protected BaseJob(JobEventObservable jobObservableHelper, String... tags) {
        super(jobObservableHelper);

        mTagsLock = new ReentrantReadWriteLock(false);
        mPauseLock = new ReentrantLock(false);

        if (tags != null && tags.length > 0) {
            mTags = new ArrayList<String>(Arrays.asList(tags));
        }

        mExecutionHandler = new ExecutionHandler() {
            @Override
            public void onPreExecute() {
                BaseJob.this.onPreExecute();
            }

            @Override
            public JobEvent execute() {
                return BaseJob.this.execute();
            }

            @Override
            public void onPostExecute(JobEvent executionResult) {
                BaseJob.this.onPostExecute(executionResult);
            }

            @Override
            public void onExceptionCaughtBase(Exception e) {
                BaseJob.this.onExceptionCaughtBase(e);
            }

            @Override
            public JobEvent executeImpl() throws Exception {
                return BaseJob.this.executeImpl();
            }
        };
    }

    protected ExecutionHandler getExecutionHandler() {
        return mExecutionHandler;
    }

    protected void setExecutionHandler(ExecutionHandler mExecutionHandler) {
        this.mExecutionHandler = mExecutionHandler;
    }

    @Override
    public boolean isJobIdAssigned() {
        return mId != JobManager.JOB_ID_UNSPECIFIED;
    }

    @Override
    public void setJobId(int jobId) {
        if (isJobIdAssigned()) {
            throw new IllegalStateException("job id is already assigned");
        }

        mId = jobId;
    }

    @Override
    public boolean isFinished() {
        return mIsFinished;
    }

    @Override
    public int getJobId() {
        return mId;
    }

    @Override
    public Future<JobEvent> getFutureResult() {
        return new JobFutureResult(this);
    }

    @Override
    public void setStatus(JobStatus status) {
        if (mStatus == status) return;

        setStatusSilent(status);
        notifyJobEvent(new JobEvent(
                JobEvent.EVENT_CODE_UPDATE,
                JobEvent.EXTRA_CODE_STATUS_CHANGED,
                mStatus));
    }

    protected void setStatusSilent(JobStatus status) {
        mStatus = status;
    }

    @Override
    public JobStatus getStatus() {
        return mStatus;
    }

    @Override
    public void setPaused(boolean isPaused) {
        mPauseLock.lock();

        try {
            if (mIsPaused != null) {
                if (mIsPaused == isPaused) {
                    return;
                }

                if (mPauseLatch != null && mPauseLatch.getCount() > 0) {
                    mPauseLatch.countDown();
                }
            }

            mIsPaused = isPaused;

        } finally {
            mPauseLock.unlock();
        }
    }

    @Override
    public boolean isPaused() {
        mPauseLock.lock();

        try {
            return mIsPaused == null ? false : mIsPaused;

        } finally {
            mPauseLock.unlock();
        }
    }

    @Override
    public void cancel() {
        if (isCancelled()) return;

        mIsCancelled = true;
        onCancelled();
    }

    @Override
    public boolean isCancelled() {
        return mIsCancelled || mStatus == JobStatus.CANCELLED;
    }

    @Override
    public void setPayload(Object payload) {
        mPayload = payload;
    }

    @Override
    public boolean hasPayload() {
        return mPayload != null;
    }

    @Override
    public Object getPayload() {
        return mPayload;
    }

    @Override
    public void addTag(String tag) {
        final Lock lock = mTagsLock.writeLock();

        lock.lock();
        try {
            addTagImpl(tag);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void addTags(String... tags) {
        if (tags == null || tags.length == 0) return;

        final Lock lock = mTagsLock.writeLock();

        lock.lock();
        try {
            for (String tag : tags) {
                addTagImpl(tag);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void addTags(Collection<String> tags) {
        if (tags == null || tags.isEmpty()) return;

        final Lock lock = mTagsLock.writeLock();

        lock.lock();
        try {
            for (String tag : tags) {
                addTagImpl(tag);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<String> getTags() {
        final Lock lock = mTagsLock.readLock();

        lock.lock();
        try {

            if (mTags == null) {
                return Collections.emptyList();
            }

            return Collections.unmodifiableList(mTags);

        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean hasTag(String tag) {
        final Lock lock = mTagsLock.readLock();

        lock.lock();
        try {

            return mTags != null && mTags.contains(tag);

        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean hasTags(String... tags) {
        if (tags == null || tags.length == 0) return false;

        final Lock lock = mTagsLock.readLock();

        lock.lock();
        try {

            if (mTags == null) return false;

            for (String tag : tags) {
                if (mTags.contains(tag) == false)
                    return false;
            }

            return true;

        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean hasTags(Collection<String> tags) {
        if (tags == null || tags.isEmpty()) return false;

        final Lock lock = mTagsLock.readLock();

        lock.lock();
        try {

            if (mTags == null) return false;

            return mTags.containsAll(tags);

        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setGroupId(int groupId) {
        mGroupId = groupId;
    }

    @Override
    public int getGroupId() {
        return mGroupId;
    }

    @Override
    public void setPriority(int priority) {
        mPriority = priority;
    }

    @Override
    public int getPriority() {
        return mPriority;
    }

    @Override
    public final void reset() {
        if (getStatus() != JobStatus.PENDING && isFinished() == false) {
            throw new IllegalStateException(String.format(
                    "can't reset unfinished job; \"%s\"", this));
        }

        onReset();

        super.reset();

        mId = JobManager.JOB_ID_UNSPECIFIED;
        mGroupId = JobManager.JOB_GROUP_DEFAULT;
        mStatus = JobStatus.PENDING;
        mIsFinished = false;
        mPriority = 0;
        mPayload = null;
        mTags = null;
        mIsCancelled = false;
        mIsPaused = null;
        mPauseLatch = null;
    }

    protected void onReset() {
        throw new UnsupportedOperationException(String.format(
                "One should override onReset() in order to reset job; \"%s\"", this));
    }

    @Override
    public JobEvent execute() {
        setStatus(JobStatus.IN_PROGRESS);

        JobEvent jobEvent;
        try {
            boolean isUnlocked = false;
            try {
                mPauseLock.lock();

                if (mIsPaused != null && mIsPaused) {
                    mPauseLatch = new CountDownLatch(1);
                    mPauseLock.unlock();
                    isUnlocked = true;
                    mPauseLatch.await();
                }

            } finally {
                if (!isUnlocked) {
                    mPauseLock.unlock();
                }
            }

            mExecutionHandler.onPreExecute();

            jobEvent = mExecutionHandler.executeImpl();

            if (jobEvent == null) {
                throw new JobExecutionException("job execution result cannot be null");
            }

            if (jobEvent.isJobIdAssigned()) {
                throw new JobExecutionException("job execution result may not define job id; " +
                        "make sure to not return another job's result from this job");
            }

            final int resultCode = jobEvent.getEventCode();
            final JobStatus status = jobEvent.getJobStatus();

            if (status != JobStatus.OK && status != JobStatus.FAILED) {
                throw new JobExecutionException(String.format("job should result in '%s' or '%s' " +
                        "status; result status: '%s'", JobStatus.OK, JobStatus.FAILED, status));
            }

            if ((resultCode == JobEvent.EVENT_CODE_OK && status == JobStatus.FAILED)
                    || (resultCode == JobEvent.EVENT_CODE_FAILED && status == JobStatus.OK)) {

                throw new JobExecutionException(String.format("inconsistent job result code " +
                        "and status returned; result: %d, status: %s", resultCode, status));
            }

            jobEvent.setJobId(getJobId());
            jobEvent.setJobGroupId(getGroupId());
            jobEvent.setJob(this);
            jobEvent.setJobTags(getTags());
            jobEvent.setJobStatus(status);

            mExecutionHandler.onPostExecute(jobEvent);

        } catch (Exception e) {
            mExecutionHandler.onExceptionCaughtBase(e);

            if (isCancelled() || Thread.interrupted()) {
                mIsCancelled = true;
                setStatusSilent(JobStatus.CANCELLED);

                jobEvent = new JobEvent(JobEvent.EVENT_CODE_CANCELLED);
                mIsFinished = true;
                notifyJobEvent(jobEvent);

                return jobEvent;
            }

            setStatusSilent(JobStatus.FAILED);
            jobEvent = new JobEvent(JobEvent.EVENT_CODE_FAILED);
            jobEvent.setUncaughtException(e);

            mIsFinished = true;
            notifyJobEvent(jobEvent);

            return jobEvent;
        }

        if (isCancelled()) {
            setStatusSilent(JobStatus.CANCELLED);
            jobEvent.setEventCode(JobEvent.EVENT_CODE_CANCELLED);
        } else {
            setStatusSilent(jobEvent.getJobStatus());
        }

        mIsFinished = true;
        notifyJobEvent(jobEvent);

        return jobEvent;
    }

    @Override
    public void run() {
        mExecutionHandler.execute();
    }

    @Override
    public JobEvent call() throws Exception {
        return mExecutionHandler.execute();
    }

    @Override
    public void notifyJobEvent(JobEvent event) {
        event.setJobId(mId);
        event.setJobGroupId(mGroupId);
        event.setJobTags(mTags);
        event.setJobStatus(mStatus);
        event.setJobFinished(mIsFinished);
        event.setJob(this);

        notifyJobEventImpl(event);
    }

    protected void notifyJobEventImpl(JobEvent event) {
        super.notifyJobEvent(event);
    }

    /**
     * Construct job event with specified extra message and
     * event code of {@link JobEvent#EVENT_CODE_UPDATE} and extra code
     * {@link JobEvent#EXTRA_CODE_STATUS_MESSAGE_CHANGED}
     * @param message message to send
     */
    protected void notifyStatusMessageUpdate(String message) {
        JobEvent event = new JobEvent(
                JobEvent.EVENT_CODE_UPDATE,
                JobEvent.EXTRA_CODE_STATUS_MESSAGE_CHANGED,
                getStatus());
        event.setExtraMessage(message);
        notifyJobEvent(event);
    }

    protected void onCancelled() {
    }

    protected void onPreExecute() {
    }

    private void addTagImpl(String tag) {
        if (tag == null) {
            throw new IllegalArgumentException("tag == null");
        }

        if (mTags == null) {
            mTags = new ArrayList<String>(1);
        }

        if (mTags.contains(tag) == false) {
            mTags.add(tag);
        }
    }

    protected void onExceptionCaughtBase(Exception e) {
        Log.e(LOG_TAG, String.format("exception caught while executing job %s", this));
        e.printStackTrace();

        try {
            onExceptionCaught(e);

        } catch (Exception e2) {
            Log.e(LOG_TAG, String.format("exception caught from uncaught exception handler; %s", this));
            e2.printStackTrace();
        }
    }

    protected void onExceptionCaught(Exception e) {
    }

    protected void onPostExecute(JobEvent executionResult) {
    }

    protected abstract JobEvent executeImpl() throws Exception;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseJob baseJob = (BaseJob) o;

        if (mGroupId != baseJob.mGroupId) return false;
        if (mId != baseJob.mId) return false;
        if (mPriority != baseJob.mPriority) return false;
        if (mPayload != null ? !mPayload.equals(baseJob.mPayload) : baseJob.mPayload != null)
            return false;
        if (mStatus != baseJob.mStatus) return false;
        if (mTags != null ? !mTags.equals(baseJob.mTags) : baseJob.mTags != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = mId;
        result = 31 * result + mGroupId;
        result = 31 * result + mPriority;
        result = 31 * result + mStatus.hashCode();
        result = 31 * result + (mPayload != null ? mPayload.hashCode() : 0);
        result = 31 * result + (mTags != null ? mTags.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "mId=" + mId +
                ", mGroupId=" + mGroupId +
                ", mPriority=" + mPriority +
                ", mStatus=" + mStatus +
                ", mTags=" + mTags +
                ", mIsCancelled=" + mIsCancelled +
                ", mPayload=" + mPayload +
                ", mTags=" + mTags +
                ", mIsPaused=" + mIsPaused +
                '}';
    }
}
