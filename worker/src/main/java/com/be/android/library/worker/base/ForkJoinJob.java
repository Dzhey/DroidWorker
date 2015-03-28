package com.be.android.library.worker.base;

import android.util.Log;
import android.util.SparseArray;

import com.be.android.library.worker.controllers.JobManager;
import com.be.android.library.worker.exceptions.JobExecutionException;
import com.be.android.library.worker.interfaces.Job;
import com.be.android.library.worker.interfaces.JobEventListener;
import com.be.android.library.worker.models.JobFutureResultStub;
import com.be.android.library.worker.models.JobId;
import com.be.android.library.worker.util.JobEventFilter;
import com.be.android.library.worker.util.JobFutureEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
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
        int getJobId();
    }

    /**
     * JobFutureResult mapped to submitted job
     */
    private final SparseArray<Future<JobEvent>> mPendingResults;
    private final ReadWriteLock mLock;
    private final LinkedList<JobId> mParentJobsPath;
    private boolean shouldForwardForkEventsByDefault;

    protected ForkJoinJob() {
        this(false, (String[]) null);
    }

    protected ForkJoinJob(String... tags) {
        this(false, tags);
    }

    protected ForkJoinJob(boolean shouldForwardForkEventsByDefault) {
        this(shouldForwardForkEventsByDefault, (String[]) null);
    }

    protected ForkJoinJob(boolean shouldForwardForkEventsByDefault, String... tags) {
        super(tags);

        mPendingResults = new SparseArray<Future<JobEvent>>();
        mLock = new ReentrantReadWriteLock(false);
        mParentJobsPath = new LinkedList<JobId>();
        this.shouldForwardForkEventsByDefault = shouldForwardForkEventsByDefault;
    }

    @Override
    public void onReset() {
        mPendingResults.clear();
        mParentJobsPath.clear();
        shouldForwardForkEventsByDefault = false;
    }

    public boolean shouldForwardForkEventsByDefault() {
        return shouldForwardForkEventsByDefault;
    }

    public void setShouldForwardForkEventsByDefault(boolean shouldForwardForkEventsByDefault) {
        this.shouldForwardForkEventsByDefault = shouldForwardForkEventsByDefault;
    }

    /**
     * Set parent groups path
     * @param parents list of parent group identifiers
     */
    private void setParentJobsPath(Collection<JobId> parents) {
        mParentJobsPath.clear();
        mParentJobsPath.addAll(parents);
    }

    /**
     * Retrieve parent groups path
     */
    protected List<JobId> getParentJobsPath() {
        return Collections.unmodifiableList(mParentJobsPath);
    }

    protected JobId findParentForGroupId(int groupId) {
        final Lock lock = mLock.readLock();
        lock.lock();

        try {

            for (JobId id : mParentJobsPath) {
                if (id.getJobGroupId() == groupId) {
                    return id;
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
     * Similar to {@link #forkJob(ForkJoinJob)} but job will execute on specified group
     * @param jobGroupId job group to associate specified job with
     * @param job job to submit or execute
     * @return pending job result
     */
    protected ForkJoiner forkJob(int jobGroupId, ForkJoinJob job) throws JobExecutionException {
        return buildFork(job)
                .setForwardEvents(shouldForwardForkEventsByDefault())
                .groupOn(jobGroupId)
                .fork();
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
        return buildFork(forkJob)
                .setForwardEvents(shouldForwardForkEventsByDefault())
                .fork();
    }

    public boolean isValidForkGroup(int forkGroupId) {
        return forkGroupId == getGroupId()
                || JobManager.isSpecialJobGroup(forkGroupId)
                || JobManager.isDefaultJobGroup(forkGroupId)
                || findParentForGroupId(forkGroupId) == null;
    }

    private ForkJoiner forkJobImpl(ForkBuilder forkBuilder) throws JobExecutionException {
        if (isJobIdAssigned() == false) {
            throw new IllegalStateException(String.format("Cannot fork unbound job '%s'. " +
                    "Please submit job on job manager before trying to fork.", this));
        }

        final ForkJoinJob forkJob = forkBuilder.getJob();
        final int groupId = getGroupId();
        final int forkGroupId = forkJob.getGroupId();

        final Lock lock = mLock.writeLock();
        lock.lock();
        try {
            final List<JobId> parentsPath = new ArrayList<JobId>(getParentJobsPath());
            if (isValidForkGroup(forkGroupId) == false) {
                throw new IllegalArgumentException(String.format(
                        "One or more fork job '%s' parents '%s' share the same group id '%s'; " +
                                "this way fork job may not be submitted as it would have " +
                                "to execute after it's parent complete execution itself." +
                                "Please review your fork job tree.",
                        forkJob,
                        parentsPath,
                        groupId));
            }
            parentsPath.add(JobId.of(this));
            forkJob.setParentJobsPath(parentsPath);

            Future<JobEvent> pendingResult;
            if (JobManager.isSpecialJobGroup(forkGroupId) == false &&
                    (forkGroupId == groupId || JobManager.isDefaultJobGroup(forkGroupId))) {

                // Execute job with the same or default group id immediately
                if (forkJob.isJobIdAssigned() == false) {
                    forkJob.setJobId(generateJobId());
                }
                pendingResult = new JobFutureResultStub(forkJob.execute());

            } else {
                final Object forkPauseToken = new Object();
                final JobFutureEvent futureEvent = new JobFutureEvent(forkJob,
                        new JobEventFilter.Builder()
                                .pendingEventCode(JobEvent.EVENT_CODE_UPDATE)
                                .pendingExtraCode(JobEvent.EXTRA_CODE_STATUS_CHANGED)
                                .pendingStatus(JobStatus.ENQUEUED, JobStatus.SUBMITTED)
                                .build());
                forkJob.setPaused(forkPauseToken, true);
                forkJob.setExecutionFlags(forkJob.getExecutionFlags() | Job.EXECUTION_FLAG_FORCE_EXECUTE);
                pendingResult = JobManager.getInstance().submitJobForResult(forkJob);

                try {
                    futureEvent.get();

                    // Validate correct unique job execution
                    if (forkGroupId == JobManager.JOB_GROUP_UNIQUE
                            && (forkJob.getStatus() == JobStatus.PENDING
                                || forkJob.getStatus() == JobStatus.ENQUEUED)) {

                        forkJob.cancel();
                        JobManager.getInstance().discardJob(forkJob.getJobId());

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
                    forkJob.setPaused(forkPauseToken, false);
                }
            }

            mPendingResults.append(forkJob.getJobId(), pendingResult);

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
            forkEvent.setJobId(getJobId());
            forkEvent.setJobStatus(getStatus());
            forkEvent.setJobTags(getTags());
            forkEvent.setJobGroupId(getGroupId());
            notifyJobEventImpl(forkEvent);
        }
    }

    /**
     * Await until specified job complete it's execution and return result.
     *
     * @param joinJob job to join
     * @return fork job result
     * @throws JobExecutionException thrown on InterruptedException or ExecutionException
     */
    protected JobEvent joinJob(ForkJoinJob joinJob) {
        Future<JobEvent> pendingResult = findPendingResult(joinJob);

        if (pendingResult == null) {
            throw new IllegalArgumentException(String.format(
                    "no pending result for job '%s' found", joinJob));
        }

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

    protected Future<JobEvent> findPendingResult(ForkJoinJob forkJob) {
        final Lock lock = mLock.readLock();
        lock.lock();
        try {
            return mPendingResults.get(forkJob.getJobId());

        } finally {
            lock.unlock();
        }
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

        public ForkBuilder groupOn(int jobGroupId) {
            job.setGroupId(jobGroupId);

            return this;
        }

        public ForkBuilder groupOnTheSameGroup() {
            groupOn(JobManager.JOB_GROUP_DEFAULT);

            return this;
        }

        ForkJoinJob getJob() {
            return job;
        }

        public ForkBuilder setForwardEvents(boolean shouldForwardEvents) {
            if (!shouldForwardEvents) {
                job.removeJobEventListener(mForwardEventListener);

            } else {
                if (job.hasJobEventListener(mForwardEventListener) == false) {
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
