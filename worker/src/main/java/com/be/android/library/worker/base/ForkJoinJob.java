package com.be.android.library.worker.base;

import android.util.Log;
import android.util.SparseArray;

import com.be.android.library.worker.controllers.JobManager;
import com.be.android.library.worker.exceptions.JobExecutionException;
import com.be.android.library.worker.interfaces.JobEventListener;
import com.be.android.library.worker.models.Flag;
import com.be.android.library.worker.models.JobFutureResultStub;
import com.be.android.library.worker.models.JobParams;
import com.be.android.library.worker.models.Params;
import com.be.android.library.worker.util.JobEventFilter;
import com.be.android.library.worker.util.JobFutureEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class ForkJoinJob extends BaseJob {

    private static final String LOG_TAG = ForkJoinJob.class.getSimpleName();

    public interface ForkJoiner extends Future<JobEvent> {
        JobEvent join();
        JobEvent joinForSuccess();
        int getJobId();
    }

    /**
     * JobFutureResult mapped to submitted job
     */
    private final SparseArray<Future<JobEvent>> mPendingResults;
    private final ReadWriteLock mLock;
    private final LinkedList<JobParams> mParentJobsPath;

    protected ForkJoinJob() {
        mPendingResults = new SparseArray<Future<JobEvent>>();
        mLock = new ReentrantReadWriteLock(false);
        mParentJobsPath = new LinkedList<JobParams>();
    }

    @Override
    public void onReset() {
        mPendingResults.clear();
        mParentJobsPath.clear();
    }

    /**
     * Check specified flag within this job or it's parents
     * @param flag unique name of the flag
     * @return false if flag was not found or flag value otherwise
     */
    public boolean checkFlag(String flag) {
        if (getParams().hasFlag(flag)) {
            return getParams().checkFlag(flag);
        }

        for (JobParams params : mParentJobsPath) {
            if (params.hasFlag(flag)) {
                return params.checkFlag(flag);
            }
        }

        return false;
    }

    /**
     * Check if job has specified flag within this instance or it's parents
     * @param flag unique name of the flag
     * @return true if flag found, false otherwise
     */
    public boolean hasFlag(String flag) {
        if (getParams().hasFlag(flag)) {
            return true;
        }

        for (JobParams params : mParentJobsPath) {
            if (params.hasFlag(flag)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Find extra value for specified key within this job and it's parents
     * @param key a key for which value is mapped
     * @return null or value if any
     */
    public Object findExtra(String key) {
        final Object extra = getParams().getExtra(key);

        if (extra != null) {
            return extra;
        }

        for (JobParams params : mParentJobsPath) {
            final Object parentExtra = params.getExtra(key);

            if (parentExtra != null) {
                return parentExtra;
            }
        }

        return null;
    }

    /**
     * Find extra value for specified key within this job and it's parents
     * @param key a key for which value is mapped
     * @return true if extra has been found
     */
    public boolean hasExtra(String key) {
        if (getParams().hasExtra(key)) {
            return true;
        }

        for (JobParams params : mParentJobsPath) {
            if (params.hasExtra(key)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Find extra value for specified key within this job and it's parents
     * @param key a key for which value is mapped
     * @return null or value if any
     */
    public <T> T findExtra(String key, T defaultValue) {
        try {
            final T extra = (T) findExtra(key);

            if (extra == null) {
                return defaultValue;
            }

            return extra;

        } catch (ClassCastException e) {
            throw new RuntimeException("failed to cast extra param for key '" + key + "'", e);
        }
    }

    /**
     * Set parents params
     * @param parents list of parent group params
     */
    private void setParentJobsPath(Collection<JobParams> parents) {
        mParentJobsPath.clear();
        mParentJobsPath.addAll(parents);
    }

    /**
     * Retrieve parents params
     */
    protected List<JobParams> getParentJobsPath() {
        return Collections.unmodifiableList(mParentJobsPath);
    }

    protected JobParams findParentForGroupId(int groupId) {
        final Lock lock = mLock.readLock();
        lock.lock();

        try {
            for (JobParams params : mParentJobsPath) {
                if (params.getGroupId() == groupId) {
                    return params;
                }
            }

        } finally {
            lock.unlock();
        }

        return null;
    }

    protected ForkBuilder buildFork(ForkJoinJob job) {
        return new ForkBuilder(job);
    }

    /**
     * Execute specified job asynchronously.
     * Job should have different group id to execute in parallel with caller job.
     * Same group id will cause job to execute immediately except
     * when {@link JobManager#JOB_GROUP_UNIQUE} or {@link JobManager#JOB_GROUP_DEDICATED} applied.
     *
     * @param forkJob job to submit or execute
     * @return pending job result
     */
    protected ForkJoiner forkJob(ForkJoinJob forkJob) throws JobExecutionException {
        return buildFork(forkJob).fork();
    }

    public boolean isValidForkGroup(int forkGroupId) {
        return forkGroupId == getParams().getGroupId()
                || JobManager.isSpecialJobGroup(forkGroupId)
                || JobManager.isDefaultJobGroup(forkGroupId)
                || findParentForGroupId(forkGroupId) == null;
    }

    private ForkJoiner forkJobImpl(ForkBuilder forkBuilder) throws JobExecutionException {
        if (!hasParams()) {
            throw new IllegalStateException(String.format("Cannot fork unbound job '%s'. " +
                    "Please submit job on job manager before trying to fork.", this));
        }

        final ForkJoinJob forkJob = forkBuilder.getJob();
        if (!forkJob.hasParams()) {
            forkJob.setup().apply();
        }

        final int groupId = getParams().getGroupId();
        final int forkGroupId = forkJob.getParams().getGroupId();

        final Lock lock = mLock.writeLock();
        lock.lock();
        try {
            final List<JobParams> parentsPath = new ArrayList<JobParams>(getParentJobsPath());
            if (!isValidForkGroup(forkGroupId)) {
                throw new IllegalArgumentException(String.format(
                        "One or more fork job '%s' parents '%s' share the same group id '%s'; " +
                                "this way fork job may not be submitted as it would have " +
                                "to execute after it's parent complete execution itself." +
                                "Please review your fork job tree.",
                        forkJob,
                        parentsPath,
                        groupId));
            }
            parentsPath.add(getParams());
            forkJob.setParentJobsPath(parentsPath);

            Future<JobEvent> pendingResult;
            if (!JobManager.isSpecialJobGroup(forkGroupId) &&
                    (forkGroupId == groupId || JobManager.isDefaultJobGroup(forkGroupId))) {

                // Execute job with the same or default group id immediately
                if (!forkJob.hasParams()) {
                    forkJob.setup().apply();
                } else {
                    if (forkJob.getParams().isJobIdAssigned()) {
                        throw new IllegalArgumentException("fork job already has assigned job id");
                    }
                }
                forkJob.getParams().assignJobId(generateJobId());
                pendingResult = new JobFutureResultStub(forkJob.execute());

            } else {
                final JobFutureEvent futureEvent = new JobFutureEvent(forkJob,
                        new JobEventFilter.Builder()
                                .pendingEventCode(JobEvent.EVENT_CODE_UPDATE)
                                .pendingExtraCode(JobEvent.EXTRA_CODE_FLAG_STATUS_CHANGED)
                                .pendingFlags(
                                        Flag.create(Params.FLAG_JOB_ENQUEUED, true),
                                        Flag.create(Params.FLAG_JOB_SUBMITTED, true))
                                .build());
                forkJob.pause();

                forkJob.getParams().setFlag(JobParams.FLAG_FORCE_EXECUTE, true);
                pendingResult = getJobManager().submitJobForResult(forkJob);

                try {
                    futureEvent.get();

                    // Validate correct unique job execution
                    if (forkGroupId == JobManager.JOB_GROUP_UNIQUE
                            && forkJob.getParams().checkFlag(JobParams.FLAG_JOB_SUBMITTED) == false) {

                        forkJob.cancel();
                        getJobManager().discardJob(forkJob.getParams().getJobId());

                        throw new JobExecutionException(String.format(
                                "Unable to execute unique-grouped job fork '%s'. " +
                                        "It may be the consequence of insufficient thread pool " +
                                        "size whilst all the pool threads are busy and unable to " +
                                        "execute another fork immediately instead of queuing it. " +
                                        "This way fork job may never be executed. " +
                                        "You may increase thread pool size, use another defined job group " +
                                        "or execute fork in the same worker thread", forkJob));
                    }

                } catch (InterruptedException e) {
                    throw new JobExecutionException(e);

                } catch (ExecutionException e) {
                    throw new JobExecutionException(e);

                } finally {
                    forkJob.unpause();
                }
            }

            mPendingResults.append(forkJob.getParams().getJobId(), pendingResult);

            return new ForkJoinerImpl(forkJob, pendingResult);

        } finally {
            lock.unlock();
        }
    }

    private int generateJobId() {
        return (int) (Math.random() * -1000000);
    }

    protected boolean onForwardJobEvent(JobEvent event) {
        // Avoid forwarding base updates
        final int eventCode = event.getEventCode();
        if (eventCode == JobEvent.EXTRA_CODE_STATUS_CHANGED ||
                eventCode == JobEvent.EXTRA_CODE_PROGRESS_UPDATE) {

            return false;
        }

        return true;
    }

    private void forwardJobEvent(JobEvent event) {
        // Do not forward result events of forwarded jobs to prevent
        // false interpret for parent job finish event
        if (event.isJobFinished()) return;

        if (onForwardJobEvent(event)) {
            JobEvent forkEvent = new JobEvent(event);
            forkEvent.setJobParams(getParams());
            forkEvent.setJobStatus(getStatus());
            notifyJobEventImpl(forkEvent);
        }
    }

    /**
     * Join all forked jobs and return result events
     * @return
     */
    protected List<JobEvent> joinAll() {
        final List<JobEvent> results = new ArrayList<JobEvent>();

        final Lock lock = mLock.readLock();
        lock.lock();

        try {
            final int sz = mPendingResults.size();
            for (int i = 0; i < sz; i++) {
                results.add(joinJob(mPendingResults.keyAt(i)));
            }

        } finally {
            lock.unlock();
        }

        return results;
    }

    /**
     * Await until specified job complete it's execution and return result.
     *
     * @param joinJob job to join
     * @return fork job result
     */
    protected JobEvent joinJob(ForkJoinJob joinJob) {
        final Future<JobEvent> pendingResult = findPendingResult(joinJob);

        if (pendingResult == null) {
            throw new IllegalArgumentException(String.format(
                    "no pending result for job '%s' found", joinJob));
        }

        return joinJobImpl(pendingResult);
    }

    /**
     * Await until specified job complete it's execution and return result.
     *
     * @param forkJobId job to join
     * @return fork job result
     */
    protected JobEvent joinJob(int forkJobId) {
        final Future<JobEvent> pendingResult = mPendingResults.get(forkJobId);

        if (pendingResult == null) {
            throw new IllegalArgumentException(String.format(
                    "no pending result for job id:'%s' found", forkJobId));
        }

        return joinJobImpl(pendingResult);
    }

    private JobEvent joinJobImpl(Future<JobEvent> pendingResult) {
        try {
            return pendingResult.get();

        } catch (InterruptedException e) {
            String msg = String.format("job execution interrupted at joinJob; %s", this);
            Log.w(LOG_TAG, msg);
            throw new RuntimeException(msg, e);

        } catch (ExecutionException e) {
            String msg = String.format("job execution exception caught at joinJob; %s", this);
            Log.e(LOG_TAG, msg);
            throw new RuntimeException(msg, e);
        }
    }

    /**
     * Await until specified job complete it's execution and return result.
     * if result job status doesn't meet {@link JobStatus#OK}
     * then {@link JobExecutionException} will be thrown
     * @param job
     * @throws JobExecutionException if result unsuccessful
     */
    protected JobEvent joinJobForSuccess(ForkJoinJob job) throws JobExecutionException {
        final JobEvent resultEvent = joinJob(job);

        switch (resultEvent.getJobStatus()) {
            case CANCELLED:
                throw new JobExecutionException(String.format(
                        "job '%s' has been cancelled", job.getClass().getSimpleName()));

            case FAILED:
                throw new JobExecutionException(String.format("job '%s' result unsuccessful",
                        job.getClass().getSimpleName()));

            case OK:
                break;

            default:
                throw new JobExecutionException(String.format(
                        "unexpected result job status: %s", resultEvent.getJobStatus()));
        }

        return resultEvent;
    }

    protected Future<JobEvent> findPendingResult(ForkJoinJob forkJob) {
        final JobParams params = forkJob.getParams();

        if (params == null || !params.isJobIdAssigned()) {
            return null;
        }

        return findPendingResult(params.getJobId());
    }

    protected Future<JobEvent> findPendingResult(int jobId) {
        final Lock lock = mLock.readLock();
        lock.lock();

        if (mPendingResults.indexOfKey(jobId) < 0) {
            return null;
        }

        try {
            return mPendingResults.get(jobId);

        } finally {
            lock.unlock();
        }
    }

    protected JobManager getJobManager() {
        return JobManager.getInstance();
    }

    public class ForkBuilder {

        private final ForkJoinJob job;

        private final JobEventListener mForwardEventListener = new JobEventListener() {
            @Override
            public void onJobEvent(JobEvent event) {
                ForkJoinJob.this.forwardJobEvent(event);
            }
        };

        private ForkBuilder(ForkJoinJob job) {
            this.job = job;
        }

        ForkJoinJob getJob() {
            return job;
        }

        public ForkBuilder setForwardEvents(boolean shouldForwardEvents) {
            if (!shouldForwardEvents) {
                job.removeJobEventListener(mForwardEventListener);

            } else {
                if (!job.hasJobEventListener(mForwardEventListener)) {
                    job.addJobEventListener(mForwardEventListener);
                }
            }

            return this;
        }

        public ForkBuilder forwardEvents() {
            setForwardEvents(true);

            return this;
        }

        public ForkJoiner fork() throws JobExecutionException {
            return ForkJoinJob.this.forkJobImpl(this);
        }
    }

    private class ForkJoinerImpl implements ForkJoiner, Future<JobEvent> {

        private Future<JobEvent> pendingResult;
        private ForkJoinJob job;

        public ForkJoinerImpl(ForkJoinJob job, Future<JobEvent> pendingResult) {
            this.pendingResult = pendingResult;
            this.job = job;
        }

        @Override
        public JobEvent join() {
            return ForkJoinJob.this.joinJob(job);
        }

        @Override
        public JobEvent joinForSuccess() {
            return ForkJoinJob.this.joinJobForSuccess(job);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return pendingResult.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return pendingResult.isCancelled();
        }

        @Override
        public boolean isDone() {
            return pendingResult.isDone();
        }

        @Override
        public JobEvent get() throws InterruptedException, ExecutionException {
            return pendingResult.get();
        }

        @Override
        public JobEvent get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return pendingResult.get(timeout, unit);
        }

        @Override
        public int getJobId() {
            return job.getJobId();
        }
    }
}
