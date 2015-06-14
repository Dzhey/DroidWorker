package com.be.android.library.worker.controllers;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.SparseArray;

import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.base.JobStatus;
import com.be.android.library.worker.base.ProfilerJob;
import com.be.android.library.worker.interfaces.Job;
import com.be.android.library.worker.interfaces.JobEventListener;
import com.be.android.library.worker.interfaces.Worker;
import com.be.android.library.worker.models.JobParams;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ThreadPoolWorker implements Worker {

    public static final String LOG_TAG = ThreadPoolWorker.class.getSimpleName();

    public static final String BASE_THREAD_NAME = "PoolWorkerThread.";
    public static final String BASE_EXCLUSIVE_JOB_THREAD_NAME = "ExclusiveJobThread.";
    public static final String BASE_ALLOCATED_THREAD_NAME = "AllocatedWorkerThread.";
    public static final String BASE_DEDICATED_THREAD_NAME = "DedicatedWorkerThread.";

    private static final int THREAD_KEEP_ALIVE_TIME_MILLIS = 10000;
    private static final int MAX_FREE_EXCLUSIVE_EXECUTORS_DEFAULT = 10;
    private static final int MAX_FREE_EXCLUSIVE_EXECUTORS_UNLIMITED = -1;

    private final ThreadFactory mCoreThreadFactory = new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            String threadName = BASE_THREAD_NAME +
                    mPoolThreadCounter.incrementAndGet() +
                    "(" +
                    mThreadCounter.incrementAndGet() +
                    ")";

            Thread thread = new Thread(runnable, threadName);

            logTrace("+ created new pool thread '%s'", threadName);

            return thread;
        }
    };

    private class ExclusiveJobThreadFactory implements ThreadFactory {

        private ExclusiveJobThreadFactory() {
        }

        @Override
        public Thread newThread(Runnable runnable) {
            String threadName = BASE_EXCLUSIVE_JOB_THREAD_NAME +
                    mExclusiveThreadCounter.incrementAndGet() +
                    "(" +
                    mThreadCounter.incrementAndGet() +
                    ")";

            Thread thread = new Thread(runnable, threadName);
            logTrace("+ created new exclusive thread '%s'", threadName);

            return thread;
        }
    }

    private class AllocatedThreadFactory implements ThreadFactory {

        private int jobGroupId;

        private AllocatedThreadFactory(int jobGroupId) {
            this.jobGroupId = jobGroupId;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            String threadName = BASE_ALLOCATED_THREAD_NAME +
                    mAllocatedThreadCounter.incrementAndGet() +
                    "(" +
                    mThreadCounter.incrementAndGet() +
                    ")[group-id:" +
                    String.valueOf(jobGroupId) +
                    "]";

            Thread thread = new Thread(runnable, threadName);
            logTrace("+ created new allocated thread '%s' for job group '%d'", threadName, jobGroupId);

            return thread;
        }
    }

    private class DedicatedThreadFactory implements ThreadFactory {

        private int jobId;
        private int jobGroupId;

        private DedicatedThreadFactory(int jobId, int jobGroupId) {
            this.jobId = jobId;
            this.jobGroupId = jobGroupId;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            String threadName = BASE_DEDICATED_THREAD_NAME +
                    mDedicatedThreadCounter.incrementAndGet() +
                    "(" +
                    mThreadCounter.incrementAndGet() +
                    ")[job-id:" +
                    String.valueOf(jobId) +
                    ", group-id:" +
                    String.valueOf(jobGroupId) +
                    "]";

            Thread thread = new Thread(runnable, threadName);

            logTrace("+ created new dedicated thread '%s'", threadName);

            return thread;
        }
    }

    /**
     * Main job executor
     */
    private final ThreadPoolExecutor mCoreExecutor;

    /**
     * Executors mapped to specific job group id
     */
    private final SparseArray<ExecutorProvider> mAllocatedExecutors;

    /**
     * Executors mapped to specific job id
     */
    private final SparseArray<ExecutorProvider> mExclusiveJobExecutors;

    /**
     * Queued jobs mapped to certain job group id
     *
     * Once job is executed, corresponding entry is evicted.
     *
     */
    private final SparseArray<TreeSet<Job>> mQueuedJobs;

    /**
     * Job group id mapped to each executed job.
     * Once job execution finished, corresponding entry is evicted.
     */
    private final SparseArray<Job> mPendingJobs;
    private final LinkedList<Job> mUniquePendingJobs;
    private final LinkedList<ExecutorProvider> mFreeExecutors;

    private final Handler mHandler;
    private final String mJobFinishListenerTag;
    private final ReadWriteLock mQueuedJobsLock;
    private final ReadWriteLock mPendingJobsLock;
    private final Lock mExecuteLock;
    private final Lock mDispatchLock;
    private final AtomicInteger mThreadCounter;
    private final AtomicInteger mPoolThreadCounter;
    private final AtomicInteger mAllocatedThreadCounter;
    private final AtomicInteger mDedicatedThreadCounter;
    private final AtomicInteger mExclusiveThreadCounter;
    private boolean mIsTraceEnabled;
    private boolean mIsSelectiveTraceEnabled;
    private final AtomicBoolean mIsStopped;
    private final int mCoreThreadCount;
    private int mMaximumThreadCount;
    private int mMaxFreeExclusiveExecutorsCount = MAX_FREE_EXCLUSIVE_EXECUTORS_DEFAULT;

    /**
     * Job type list defining which job types to trace if selective trace is enabled.
     * Contains fully-qualified type names.
     */
    private final Set<String> mTraceJobs;
    private final Set<Integer> mTraceJobGroups;
    private final Lock mTraceLock;
    private final BlockingQueue<Runnable> mCoreExecutorQueue;

    private final Comparator<Job> mJobPriorityComparator =
            new Comparator<Job>() {
                @Override
                public int compare(Job lhs, Job rhs) {
                    int priorityDelta = rhs.getParams().getPriority() - lhs.getParams().getPriority();
                    if (priorityDelta == 0) {
                        return lhs.getJobId() - rhs.getJobId();
                    }

                    return priorityDelta;
                }
            };

    private final JobEventListener mJobEventListener = new JobEventListener() {
        @Override
        public void onJobEvent(final JobEvent event) {
            if (event.isJobFinished() == false) return;

            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    handleJobResult(event);
                }
            });
        }
    };

    public ThreadPoolWorker(int threadCount) {
        if (threadCount < 1) {
            throw new IllegalArgumentException("threadCount < 1");
        }

        mHandler = new Handler(Looper.getMainLooper());
        mCoreThreadCount = threadCount;
        mMaximumThreadCount = mCoreThreadCount;
        mJobFinishListenerTag = getClass().getSimpleName() + "_listener_tag_" + String.valueOf(System.currentTimeMillis());
        mTraceJobs = new HashSet<String>();
        mIsStopped = new AtomicBoolean(false);
        mTraceJobGroups = new HashSet<Integer>();
        mTraceLock = new ReentrantLock(false);
        mThreadCounter = new AtomicInteger(0);
        mPoolThreadCounter = new AtomicInteger(0);
        mAllocatedThreadCounter = new AtomicInteger(0);
        mDedicatedThreadCounter = new AtomicInteger(0);
        mExclusiveThreadCounter = new AtomicInteger(0);
        mQueuedJobsLock = new ReentrantReadWriteLock();
        mPendingJobsLock = new ReentrantReadWriteLock();
        mPendingJobs = new SparseArray<Job>();
        mUniquePendingJobs = new LinkedList<Job>();
        mFreeExecutors = new LinkedList<ExecutorProvider>();
        mAllocatedExecutors = new SparseArray<ExecutorProvider>(1);
        mExclusiveJobExecutors = new SparseArray<ExecutorProvider>(0);
        mQueuedJobs = new SparseArray<TreeSet<Job>>();
        mExecuteLock = new ReentrantLock(false);
        mDispatchLock = new ReentrantLock(false);
        mCoreExecutorQueue = new SynchronousQueue<Runnable>(false);
        mCoreExecutor = new ThreadPoolExecutor(mCoreThreadCount, mCoreThreadCount,
                THREAD_KEEP_ALIVE_TIME_MILLIS, TimeUnit.MILLISECONDS, mCoreExecutorQueue, mCoreThreadFactory);

        mCoreExecutor.setMaximumPoolSize(mMaximumThreadCount);
        Log.d(LOG_TAG, String.format("thread pool worker created; core pool size: '%d'", threadCount));
    }

    /**
     * Get current number of free exclusive executors
     */
    public int getFreeExclusiveExecutorsCount() {
        mExecuteLock.lock();
        try {
            return mFreeExecutors.size();

        } finally {
            mExecuteLock.unlock();
        }
    }

    /**
     * Set maximum number of free exclusive executors to keep
     * @param count
     */
    public void setMaxFreeExclusiveExecutorsCount(int count) {
        if (count < 0 && count != MAX_FREE_EXCLUSIVE_EXECUTORS_UNLIMITED) {
            throw new IllegalArgumentException("invalid count");
        }

        if (mMaxFreeExclusiveExecutorsCount == count) {
            return;
        }

        mMaxFreeExclusiveExecutorsCount = count;

        if (count == MAX_FREE_EXCLUSIVE_EXECUTORS_UNLIMITED) {
            return;
        }

        mExecuteLock.lock();
        try {
            while (mFreeExecutors.size() > count) {
                mFreeExecutors.poll().get().shutdown();

                logTrace("truncated free executors count to %d", mFreeExecutors.size());
            }

        } finally {
            mExecuteLock.unlock();
        }
    }

    /**
     * Get maximum number of free exclusive executors to keep
     * @return
     */
    public int getMaxFreeExclusiveExecutorsCount() {
        return mMaxFreeExclusiveExecutorsCount;
    }

    public void setTraceEnabled(boolean isTraceEnabled) {
        mIsTraceEnabled = isTraceEnabled;
    }

    public boolean isTraceEnabled() {
        return mIsTraceEnabled;
    }

    public void setIsSelectiveTraceEnabled(boolean isSelectiveTraceEnabled) {
        mIsSelectiveTraceEnabled = isSelectiveTraceEnabled;
    }

    public boolean isIsSelectiveTraceEnabled() {
        return mIsSelectiveTraceEnabled;
    }

    public boolean isJobTypeTraced(Class<? extends Job> jobType) {
        mTraceLock.lock();
        try {
            return mTraceJobs.contains(jobType.getName());
        } finally {
            mTraceLock.unlock();
        }
    }

    public void setupJobTrace(Class<? extends Job> jobType) {
        mTraceLock.lock();
        try {
            mTraceJobs.add(jobType.getName());
        } finally {
            mTraceLock.unlock();
        }
    }

    public boolean removeJobTrace(Class<? extends Job> jobType) {
        mTraceLock.lock();
        try {
            return mTraceJobs.remove(jobType.getName());
        } finally {
            mTraceLock.unlock();
        }
    }

    public boolean isJobGroupTraced(int jobGroupId) {
        mTraceLock.lock();
        try {
            return mTraceJobGroups.contains(jobGroupId);
        } finally {
            mTraceLock.unlock();
        }
    }

    public void setupJobGroupTrace(int jobGroupId) {
        mTraceLock.lock();
        try {
            mTraceJobGroups.add(jobGroupId);
        } finally {
            mTraceLock.unlock();
        }
    }

    public boolean removeJobGroupTrace(int jobGroupId) {
        mTraceLock.lock();
        try {
            return mTraceJobGroups.remove(jobGroupId);
        } finally {
            mTraceLock.unlock();
        }
    }

    private void allocateExclusiveJobExecutor(int jobId) {
        mExecuteLock.lock();
        try {
            if (mExclusiveJobExecutors.indexOfKey(jobId) > -1) {
                throw new IllegalArgumentException(String.format(
                        "exclusive executor is already allocated to job id:'%d'", jobId));
            }

            ExecutorProvider provider;
            if (!mFreeExecutors.isEmpty()) {
                provider = mFreeExecutors.poll();
                logTrace("exclusive executor reused for job id:'%d'", jobId);
                logTrace("free executors count: %d", mFreeExecutors.size());
            } else {
                provider = new ExecutorProvider(new ExclusiveJobThreadFactory());
                Log.d(LOG_TAG, String.format("exclusive executor allocated for job id:'%d'", jobId));
            }

            mExclusiveJobExecutors.put(jobId, provider);

        } finally {
            mExecuteLock.unlock();
        }
    }

    private void removeExclusiveJobExecutor(int jobId) {
        mExecuteLock.lock();
        try {
            int index = mExclusiveJobExecutors.indexOfKey(jobId);
            if (index < 0) {
                return;
            }

            ExecutorProvider provider = mExclusiveJobExecutors.valueAt(index);
            mExclusiveJobExecutors.remove(jobId);

            if (mMaxFreeExclusiveExecutorsCount == MAX_FREE_EXCLUSIVE_EXECUTORS_UNLIMITED
                    || mFreeExecutors.size() < mMaxFreeExclusiveExecutorsCount) {

                mFreeExecutors.add(provider);
                logTrace("keep exclusive executor to reuse (job id:'%d')", jobId);
                logTrace("free exclusive executors count: %d", mFreeExecutors.size());
            } else {
                Log.d(LOG_TAG, String.format("exclusive executor removed for job id:'%d'", jobId));
            }

        } finally {
            mExecuteLock.unlock();
        }
    }

    /**
     * Request to execute any job with the same job group id
     * in the same single thread in a serial manner.
     *
     * Only pending (enqueued) and further submitted jobs will be affected.
     * Method call will produce new thread on this worker.
     * Only single thread per group id may be created.
     *
     * {@link JobManager#JOB_GROUP_DEDICATED} and {@link JobManager#JOB_GROUP_UNIQUE} may not
     * be used to allocate executor.
     *
     * @param jobGroupId job group id to apply executor on
     */
    public void allocateExecutor(int jobGroupId) {
        if (jobGroupId == JobManager.JOB_GROUP_DEDICATED ||
                jobGroupId == JobManager.JOB_GROUP_UNIQUE) {

            throw new IllegalArgumentException("can't allocate executor for " +
                    "JOB_GROUP_DEDICATED or JOB_GROUP_UNIQUE");
        }

        mExecuteLock.lock();
        try {
            if (mAllocatedExecutors.indexOfKey(jobGroupId) > -1) {
                throw new IllegalArgumentException(String.format(
                        "executor is already allocated to job group '%d'", jobGroupId));
            }

            mAllocatedExecutors.put(jobGroupId, new ExecutorProvider(
                    new AllocatedThreadFactory(jobGroupId)));
            Log.d(LOG_TAG, String.format("executor allocated for group: '%s'", jobGroupId));

        } finally {
            mExecuteLock.unlock();
        }
    }

    @Override
    public void submitJob(Job job) {
        logTrace(">submitting job..", job);
        checkSubmitPreconditions(job);

        if (isStopped()) {
            throw new IllegalStateException("worker is already stopped");
        }

        dispatchJob(job);
        logTrace("<job submitted", job);
    }

    /**
     * Find job that is enqueued or executing.
     *
     * @param jobId
     * @return
     */
    public Job findJobForId(int jobId) {
        Lock lock = mQueuedJobsLock.readLock();
        lock.lock();
        try {
            int sz = mQueuedJobs.size();
            for (int i = 0; i < sz; i++) {
                TreeSet<Job> queue = mQueuedJobs.valueAt(i);
                for (Job job : queue) {
                    if (job.getJobId() == jobId) {
                        return job;
                    }
                }
            }

        } finally {
            lock.unlock();
        }

        return findPendingJobForId(jobId);
    }

    /**
     * Find job that is already executing.
     * <p>
     * Jobs with group id of {@link JobManager#JOB_GROUP_DEDICATED}
     * are out of lookup scope and cannot be returned.
     *
     * @param jobId unique job identifier to perform lookup
     * @return previously executed job or null if none found
     */
    public Job findPendingJobForId(int jobId) {
        final Lock lock = mPendingJobsLock.readLock();
        lock.lock();
        try {
            for (Job job : mUniquePendingJobs) {
                if (job.getJobId() == jobId) {
                    return job;
                }
            }

            final int sz = mPendingJobs.size();
            for (int i = 0; i < sz; i++) {
                Job job = mPendingJobs.valueAt(i);
                if (job.getJobId() == jobId) {
                    return job;
                }
            }

        } finally {
            lock.unlock();
        }

        return null;
    }

    /**
     * Find job that is already executing.
     * <p>
     * Jobs with group id of {@link JobManager#JOB_GROUP_UNIQUE}
     * or {@link JobManager#JOB_GROUP_DEDICATED} are out of lookup scope and cannot be returned.
     *
     * @param jobGroupId job groupId identifier to perform lookup
     * @return previously executed job or null if none found
     */
    public Job findPendingJobForGroupId(int jobGroupId) {
        if (jobGroupId == JobManager.JOB_GROUP_UNIQUE
                || jobGroupId == JobManager.JOB_GROUP_DEDICATED) {

            return null;
        }

        final Lock lock = mPendingJobsLock.readLock();
        lock.lock();
        try {
            return mPendingJobs.get(jobGroupId, null);

        } finally {
            lock.unlock();
        }
    }

    public boolean isStopped() {
        return mIsStopped.get();
    }

    @Override
    public void finish() {
        logTrace(">stopping thread pool worker..");
        mDispatchLock.lock();
        try {
            mIsStopped.set(true);

            final Lock queuedJobsLock = mQueuedJobsLock.writeLock();
            queuedJobsLock.lock();
            try {
                mQueuedJobs.clear();

            } finally {
                queuedJobsLock.unlock();
            }

            performShutdown();

        } finally {
            mDispatchLock.unlock();
        }

        logTrace("<thread pool worker stopped");
    }

    protected void performShutdown() {
        shutdownAsync();
    }

    protected void shutdownImpl() {
        logTrace(">(async) shutdown job executors..");
        mExecuteLock.lock();
        try {
            mCoreExecutor.shutdown();
            logTrace("+ main thread pool executor stopped");
            final int sz = mAllocatedExecutors.size();
            for (int i = 0; i < sz; i++) {
                ExecutorService executor = mAllocatedExecutors.valueAt(i).get();
                executor.shutdown();
                logTrace("+ allocated executor '%d' stopped", i);
            }
            final int jobExecutorsCount = mExclusiveJobExecutors.size();
            for (int i = 0; i < jobExecutorsCount; i++) {
                ExecutorService executor = mExclusiveJobExecutors.valueAt(i).get();
                executor.shutdown();
                logTrace("+ exclusive job executor '%d' stopped", i);
            }
            logTrace("<job executors stopped");
        } finally {
            mExecuteLock.unlock();
        }
    }

    private void shutdownAsync() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                shutdownImpl();
                return null;
            }
        }.execute();
    }

    private void dispatchJob(Job job) {
        logTrace(">dispatching job..", job);

        if (isStopped()) {
            Log.e(LOG_TAG, String.format("unable to dispatch job: worker is stopped; %s", job));
            return;
        }

        final int groupId = job.getParams().getGroupId();

        mDispatchLock.lock();

        try {
            if (!job.hasJobEventListener(mJobFinishListenerTag)) {
                job.addJobEventListener(mJobFinishListenerTag, mJobEventListener);
            }

            Job pendingJob = findPendingJobForGroupId(groupId);
            if (pendingJob != null) {
                enqueueJob(job);

                logTrace("<job dispatched", job);

                return;
            }

            if (groupId == JobManager.JOB_GROUP_UNIQUE) {
                if (getPendingJobCount() >= mMaximumThreadCount) {
                    if (job.getParams().checkFlag(JobParams.FLAG_FORCE_EXECUTE)) {

                        allocateExclusiveJobExecutor(job.getJobId());
                        executeJob(job);
                    } else {
                        enqueueJob(job);
                    }
                    logTrace("<job dispatched (all threads are busy)", job);
                    return;
                }
            }

            executeJob(job);
            logTrace("<job dispatched", job);

        } finally {
            mDispatchLock.unlock();
        }
    }

    private void handleJobResult(JobEvent jobEvent) {
        final int jobId = jobEvent.getJobParams().getJobId();
        final int jobGroupId = jobEvent.getJobParams().getGroupId();

        final Job job = findPendingJobForId(jobId);
        final boolean isTraceEnabled = logTrace("+ job finished;", job);

        if (isStopped()) {
            logTrace("+ ignored job finish: worker is stopped", job);
            return;
        }

        if (jobGroupId == JobManager.JOB_GROUP_DEDICATED) {
            logTrace("+ handled dedicated job", job);
            return;
        }

        if (job != null) {
            removePendingJob(job);
            if (isTraceEnabled) {
                if (job instanceof ProfilerJob) {
                    logTraceForce("Profile job dump:\n%s", job, ((ProfilerJob) job).dumpProfile());
                }
            }

            if (job.getParams().checkFlag(JobParams.FLAG_FORCE_EXECUTE)) {
                removeExclusiveJobExecutor(job.getJobId());
            }

        } else {
            Log.w(LOG_TAG, String.format("can't find pending job to " +
                            "remove on finish: job id:'%s' not found", jobId));
        }

        // Synchronize with job dispatch to ensure serial job enqueue/deque
        mDispatchLock.lock();

        Job pendingJob = null;
        try {
            if (jobGroupId != JobManager.JOB_GROUP_UNIQUE) {
                pendingJob = getNextQueuedJobForGroupId(jobGroupId);

                if (job != null) {
                    logTrace("+ dispatching next queued job of the same group..", job);
                }
            }

            if (pendingJob == null) {
                pendingJob = getNextQueuedJob();
                if (job != null) {
                    logTrace("+ dispatching next queued job..", job);
                }
            }

            if (pendingJob != null) {
                dispatchJob(pendingJob);
            } else {
                logTrace("+ no more jobs to execute");
            }
        } finally {
            mDispatchLock.unlock();
        }
    }

    private void executeJob(Job job) {
        logTrace(">executing job..", job);
        final int groupId = job.getParams().getGroupId();
        addPendingJob(job);

        if (mIsStopped.get()) {
            Log.w(LOG_TAG, String.format("unable to execute job: thread pool worker " +
                    "has been stopped after job is dispatched; %s", job));

            return;
        }

        job.getParams().getFlags().setFlag(JobParams.FLAG_JOB_SUBMITTED, true);

        try {
            mExecuteLock.lock();

            if (groupId == JobManager.JOB_GROUP_DEDICATED) {
                logTrace("+ executing job on dedicated executor", job);
                new DedicatedThreadFactory(job.getJobId(), groupId).newThread(job).start();

            } else {
                int index = mAllocatedExecutors.indexOfKey(groupId);

                if (index > -1) {
                    logTrace("+ executing job on allocated executor", job);
                    ExecutorProvider provider = mAllocatedExecutors.get(groupId);
                    provider.get().submit((Runnable) job);

                } else {
                    index = mExclusiveJobExecutors.indexOfKey(job.getJobId());

                    if (index > -1) {
                        logTrace("+ executing job on exclusive executor", job);
                        ExecutorProvider provider = mExclusiveJobExecutors.get(job.getJobId());
                        provider.get().submit((Runnable) job);

                    } else {
                        logTrace("+ executing job on common thread pool", job);
                        mCoreExecutor.submit((Runnable) job);
                    }
                }
            }

            logTrace("<job submitted on execution", job);

        } catch (RejectedExecutionException e) {
            Log.e(LOG_TAG, String.format("unable to execute submitted job %s: " +
                    "thread pool exceeded", job));
            e.printStackTrace();
            job.notifyJobEvent(JobEvent.failure(e.getMessage(), e));

        } finally {
            mExecuteLock.unlock();
        }
    }

    private void enqueueJob(Job job) {
        logTrace(">enqueue job..", job);

        final int groupId = job.getParams().getGroupId();

        final Lock lock = mQueuedJobsLock.writeLock();
        lock.lock();
        try {

            TreeSet<Job> queue = mQueuedJobs.get(groupId);
            if (queue == null) {
                queue = new TreeSet<Job>(mJobPriorityComparator);
                mQueuedJobs.put(groupId, queue);
            }
            final int sz = queue.size();
            queue.add(job);
            if (queue.size() == sz) {
                throw new RuntimeException("job comparator collision detected");
            }

            job.getParams().getFlags().setFlag(JobParams.FLAG_JOB_ENQUEUED, true);

            logTrace("<job added to job queue", job);

        } finally {
            lock.unlock();
        }
    }

    private int getPendingJobCount() {
        final Lock lock = mPendingJobsLock.readLock();
        lock.lock();
        try {
            return mPendingJobs.size() + mUniquePendingJobs.size();

        } finally {
            lock.unlock();
        }
    }

    private void addPendingJob(Job job) {
        final int jobGroupId = job.getParams().getGroupId();
        if (jobGroupId == JobManager.JOB_GROUP_DEDICATED) {
            return;
        }

        logTrace(">add pending job..", job);

        final Lock lock = mPendingJobsLock.writeLock();

        lock.lock();
        try {
            // JobManager.JOB_GROUP_UNIQUE is also added only to adjust thread pool size
            mPendingJobs.append(jobGroupId, job);
            if (jobGroupId == JobManager.JOB_GROUP_UNIQUE) {
                mUniquePendingJobs.add(job);
            }

            int sz = mPendingJobs.size();
            if (sz > mMaximumThreadCount) {
                Log.d(LOG_TAG, String.format("+ incrementing maximum thread pool size from '%d' to '%d'..",
                        mMaximumThreadCount, sz));
                mMaximumThreadCount = sz;
                mCoreExecutor.setMaximumPoolSize(mMaximumThreadCount);
            }

        } finally {
            lock.unlock();
        }

        logTrace("<pending job added", job);
    }

    private void removePendingJob(Job job) {
        final int jobGroupId = job.getParams().getGroupId();

        if (jobGroupId == JobManager.JOB_GROUP_UNIQUE) {
            removePendingJobForUniqueGroup(job);
            return;
        }
        if (jobGroupId == JobManager.JOB_GROUP_DEDICATED) {
            throw new IllegalArgumentException(
                    "unable to remove dedicated job from pending job list");
        }

        logTrace(">remove pending job", job);

        Lock lock = mPendingJobsLock.writeLock();
        lock.lock();
        try {
            int groupIdIndex = mPendingJobs.indexOfKey(jobGroupId);

            if (groupIdIndex != -1) {
                mPendingJobs.remove(jobGroupId);

            } else {
                Log.e(LOG_TAG, String.format("unable to remove pending job: " +
                        "job is not pending; %s", job));
            }

        } finally {
            lock.unlock();
        }

        logTrace("<pending job removed", job);
    }

    private void removePendingJobForUniqueGroup(Job uniqueGroupJob) {
        logTrace(">remove unique-group pending job", uniqueGroupJob);

        if (uniqueGroupJob.getParams().getGroupId() != JobManager.JOB_GROUP_UNIQUE) {
            throw new IllegalArgumentException("job group is not unique job group");
        }

        Lock lock = mPendingJobsLock.writeLock();
        lock.lock();
        try {
            Iterator<Job> iter = mUniquePendingJobs.iterator();
            while (iter.hasNext()) {
                Job pendingJob = iter.next();
                if (pendingJob.getJobId() == uniqueGroupJob.getJobId()) {
                    iter.remove();
                    break;
                }
            }

            if (mUniquePendingJobs.isEmpty()) {
                mPendingJobs.remove(JobManager.JOB_GROUP_UNIQUE);
            }

        } finally {
            lock.unlock();
        }

        logTrace("<unique-group pending job removed", uniqueGroupJob);
    }

    private Job getNextQueuedJob() {
        final Lock lock = mQueuedJobsLock.readLock();
        lock.lock();
        try {
            int sz = mQueuedJobs.size();
            for (int i = 0; i < sz; i++) {
                TreeSet<Job> queue = mQueuedJobs.valueAt(i);

                if (queue.isEmpty()) continue;

                Job job = queue.first();
                queue.remove(job);

                return job;
            }

            return null;

        } finally {
            lock.unlock();
        }
    }

    private Job getNextQueuedJobForGroupId(int groupId) {
        final Lock lock = mQueuedJobsLock.readLock();
        lock.lock();
        try {
            TreeSet<Job> queue = mQueuedJobs.get(groupId);

            if (queue == null || queue.isEmpty()) return null;

            Job job = queue.first();
            queue.remove(job);

            return job;

        } finally {
            lock.unlock();
        }
    }

    private void checkSubmitPreconditions(Job job) {
        if (job == null) {
            throw new IllegalArgumentException("job is null");
        }
        if (job.getStatus() != JobStatus.PENDING) {
            throw new IllegalArgumentException("job is already submitted");
        }
        if (job.hasId() == false) {
            throw new IllegalArgumentException("job has no assigned job id");
        }
    }

    private void logTrace(String string, Object... args) {
        if (!mIsTraceEnabled) return;

        Log.d(LOG_TAG, String.format(string, args));
    }

    private boolean isJobTraceEnabled(Job job) {
        if (mIsSelectiveTraceEnabled) {
            if (job != null
                    && mTraceJobGroups.contains(job.getJobId()) == false
                    && mTraceJobs.contains(job.getClass().getName()) == false) {

                return false;
            }

            return true;
        }

        return mIsTraceEnabled;
    }

    /**
     * @param string format string
     * @param loggedJob job to log out
     * @param args optional arguments for String.format substitution
     * @return true if job trace is enabled
     */
    private boolean logTrace(String string, Job loggedJob, Object... args) {
        if (isJobTraceEnabled(loggedJob) == false) {
            return false;
        }

        logTraceForce(string, loggedJob, args);

        return true;
    }

    private void logTraceForce(String string, Job loggedJob, Object... args) {
        if (loggedJob != null) {
            string = String.format("%s; %s", string, loggedJob.toString());
        }

        Log.d(LOG_TAG, String.format(string, args));
    }
}
