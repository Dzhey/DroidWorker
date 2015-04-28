package com.be.android.library.worker.base;

import android.util.Log;

import com.be.android.library.worker.controllers.JobEventObservableImpl;
import com.be.android.library.worker.exceptions.JobExecutionException;
import com.be.android.library.worker.interfaces.JobEventObservable;
import com.be.android.library.worker.models.JobParams;
import com.be.android.library.worker.models.Params;
import com.be.android.library.worker.util.JobFutureResult;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class BaseJob extends JobObservable {

    protected interface ExecutionHandler {
        public void onPreExecute() throws Exception;
        public JobEvent execute();
        public void onPostExecute(JobEvent executionResult) throws Exception;
        public void onExceptionCaughtBase(Exception e);
        public JobEvent executeImpl() throws Exception;
    }

    public static final String LOG_TAG = BaseJob.class.getSimpleName();

    private static final JobEvent EVENT_OK = JobEvent.ok();

    private JobStatusHolder mStatusHolder = new JobStatusHolder();
    private AtomicInteger mPauseCounter;
    private volatile boolean mIsCancelled;
    private CountDownLatch mPauseLatch;
    private Lock mPauseLock;
    private ExecutionHandler mExecutionHandler;
    private Params mParams;

    protected BaseJob() {
        this(new JobEventObservableImpl());
    }

    protected BaseJob(JobEventObservable jobObservableHelper) {
        super(jobObservableHelper);

        mPauseLock = new ReentrantLock(false);
        mPauseCounter = new AtomicInteger(0);

        mExecutionHandler = new ExecutionHandler() {
            @Override
            public void onPreExecute() throws Exception {
                BaseJob.this.onPreExecute();
            }

            @Override
            public JobEvent execute() {
                return BaseJob.this.execute();
            }

            @Override
            public void onPostExecute(JobEvent executionResult) throws Exception {
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

    public final JobConfigurator setup() {
        return createConfigurator();
    }

    protected JobConfigurator createConfigurator() {
        final BaseJobConfigurator configurator = new BaseJobConfigurator(this);

        configurator.init();

        return configurator;
    }

    void setParams(Params params) {
        if (isPending()) {
            throw new IllegalStateException("Job already submitted");
        }

        mParams = params;
    }

    public int getJobId() {
        if (mParams == null || !mParams.isJobIdAssigned()) {
            throw new IllegalStateException("job id is not assigned");
        }

        return mParams.getJobId();
    }

    public Params getParams() {
        if (mParams == null) {
            throw new IllegalStateException("no params defined");
        }

        return mParams;
    }

    @Override
    public boolean hasId() {
        return mParams != null && mParams.isJobIdAssigned();
    }

    @Override
    public boolean hasParams() {
        return mParams != null;
    }

    @Override
    public boolean isPending() {
        return mStatusHolder.getJobStatus() == JobStatus.PENDING;
    }

    @Override
    public boolean isFinished() {
        return mStatusHolder.getJobStatus() == JobStatus.OK
                || mStatusHolder.getJobStatus() == JobStatus.FAILED;
    }

    @Override
    public boolean isFinishedOrCancelled() {
        return mIsCancelled
                || mStatusHolder.getJobStatus() == JobStatus.CANCELLED
                || isFinished();
    }

    @Override
    public Future<JobEvent> getPendingResult() {
        if (isFinished()) {
            throw new IllegalStateException("job has already finished");
        }

        return new JobFutureResult(this);
    }

    protected final void setStatus(JobStatus status) {
        if (mStatusHolder.getJobStatus() == status) return;

        setStatusSilent(status);

        onStatusChange();

        notifyJobEvent(new JobEvent(
                JobEvent.EVENT_CODE_UPDATE,
                JobEvent.EXTRA_CODE_STATUS_CHANGED,
                status));
    }

    protected void onStatusChange() {
    }

    protected void setStatusSilent(JobStatus status) {
        try {
            mStatusHolder.setJobStatus(status);
        } catch (InterruptedException e) {
            throw new RuntimeException("interrupted");
        }
    }

    @Override
    public final JobStatus getStatus() {
        return mStatusHolder.getJobStatus();
    }

    @Override
    public int pause() {
        mPauseLock.lock();

        try {
            return mPauseCounter.incrementAndGet();

        } finally {
            mPauseLock.unlock();
        }
    }

    @Override
    public int unpause() {
        mPauseLock.lock();

        if (!isPausedImpl()) {
            return 0;
        }

        try {
            final int pauseCount = mPauseCounter.decrementAndGet();

            if (pauseCount == 0) {
                mPauseLatch.countDown();
            }

            return pauseCount;

        } finally {
            mPauseLock.unlock();
        }
    }

    @Override
    public int getPauseCount() {
        return mPauseCounter.get();
    }

    @Override
    public void unpauseAll() {
        mPauseLock.lock();

        try {
            mPauseCounter.set(0);

            if (mPauseLatch != null) {
                mPauseLatch.countDown();
            }

        } finally {
            mPauseLock.unlock();
        }
    }

    @Override
    public boolean isPaused() {
        mPauseLock.lock();

        try {
            return isPausedImpl();

        } finally {
            mPauseLock.unlock();
        }
    }

    private boolean isPausedImpl() {
        return mPauseCounter.get() > 0;
    }

    @Override
    public void cancel() {
        if (isCancelled()) return;

        mIsCancelled = true;
        onCancelled();
    }

    @Override
    public boolean isCancelled() {
        return mIsCancelled || getStatus() == JobStatus.CANCELLED;
    }

    @Override
    public final void reset() {
        if (getStatus() != JobStatus.PENDING && !isFinished()) {
            throw new IllegalStateException(String.format(
                    "can't reset unfinished job; \"%s\"", this));
        }

        onReset();

        super.reset();

        setStatusSilent(JobStatus.PENDING);
        mParams = null;
        mIsCancelled = false;
        unpauseAll();
    }

    protected void onReset() {
        throw new UnsupportedOperationException(String.format(
                "One should override onReset() in order to reset job; \"%s\"", this));
    }

    private void performPauseIfPaused() throws InterruptedException {
        boolean isUnlocked = false;
        try {
            mPauseLock.lock();

            if (isPausedImpl()) {
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
    }

    private static boolean checkEventStatusIntegrity(JobEvent event) {
        final int eventCode = event.getEventCode();
        final JobStatus status = event.getJobStatus();

        return !((eventCode == JobEvent.EVENT_CODE_OK && status == JobStatus.FAILED)
                    || (eventCode == JobEvent.EVENT_CODE_FAILED && status == JobStatus.OK));
    }

    @Override
    public JobEvent execute() {
        setStatus(JobStatus.IN_PROGRESS);

        if (mParams == null) {
            JobEvent result = JobEvent.failure("job params are not defined");
            setStatusSilent(JobStatus.FAILED);
            notifyJobEvent(result);

            return result;
        }

        JobEvent result = wrappedExecute();

        if (isCancelled() || Thread.interrupted()) {
            result = new JobEvent(JobEvent.EVENT_CODE_CANCELLED, JobStatus.CANCELLED);
        }

        final JobStatus status = result.getJobStatus();
        if (status != null) {
            setStatusSilent(status);
        } else {
            setStatusSilent(JobStatus.FAILED);
        }

        notifyJobEvent(result);

        return result;
    }

    private JobEvent wrappedExecute() {
        JobEvent jobEvent;
        try {
            onPreExecuteBase();

            jobEvent = mExecutionHandler.executeImpl();

            if (jobEvent == null) {
                throw new JobExecutionException("job execution result cannot be null");
            }

            if (jobEvent.getJobParams() != null) {
                throw new JobExecutionException("job execution result may not define job params; " +
                        "make sure you don't return another job's result from this job");
            }

            final int resultCode = jobEvent.getEventCode();
            final JobStatus status = jobEvent.getJobStatus();

            if (status != JobStatus.OK
                    && status != JobStatus.FAILED
                    && status != JobStatus.CANCELLED) {

                throw new JobExecutionException(String.format("job should result in '%s', '%s' or '%s' " +
                        "status; result status: '%s'",
                        JobStatus.OK,
                        JobStatus.FAILED,
                        JobStatus.CANCELLED,
                        status));
            }

            if (!checkEventStatusIntegrity(jobEvent)) {
                throw new JobExecutionException(String.format("inconsistent job result code " +
                        "and status returned; result: %d, status: %s", resultCode, status));
            }

            jobEvent.setJobStatus(status);
            jobEvent.setJobParams(getParams());

            mExecutionHandler.onPostExecute(jobEvent);

        } catch (Exception e) {
            setStatusSilent(JobStatus.FAILED);

            if (e instanceof JobExecutionException) {
                final JobExecutionException ex = (JobExecutionException) e;
                final JobEvent exceptionEvent = ex.getJobEvent();
                if (exceptionEvent != null) {
                    final int eventCode = exceptionEvent.getEventCode();
                    final JobStatus status = exceptionEvent.getJobStatus();

                    if (eventCode == JobEvent.EVENT_CODE_OK || status == JobStatus.OK) {
                        mExecutionHandler.onExceptionCaughtBase(
                                new JobExecutionException(
                                        String.format("illegal job result " +
                                                        "code or status obtained from JobExecutionException; result: " +
                                                        "%d, status: %s",
                                                eventCode,
                                                status)
                                )
                        );

                    } else {
                        mExecutionHandler.onExceptionCaughtBase(e);

                        return exceptionEvent;
                    }
                }
            }

            mExecutionHandler.onExceptionCaughtBase(e);

            jobEvent = new JobEvent(JobEvent.EVENT_CODE_FAILED);
            jobEvent.setUncaughtException(e);

            return jobEvent;
        }

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
        if (event.getJobParams() != null) {
            throw new IllegalArgumentException("specified event already has params; " +
                    "make sure you don't return another job's result from this job");
        }

        if (mParams == null) {
            throw new IllegalArgumentException("job params are not defined");
        }

        event.setJobStatus(getStatus());
        event.setJobParams(mParams);

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

    protected final void onPreExecuteBase() throws Exception {
        performPauseIfPaused();

        mExecutionHandler.onPreExecute();
    }

    /**
     * Called before actual job execution is performed.
     * <br/>
     * May throw any exception to prevent job from execution.
     * If {@link com.be.android.library.worker.exceptions.JobExecutionException} is thrown with
     * specified {@link com.be.android.library.worker.base.JobEvent} then
     * specified event will be used as the failure job result.
     *
     * @throws Exception
     * @see #onCheckPreconditions()
     */
    protected void onPreExecute() throws Exception {
        final JobEvent result = onCheckPreconditions();

        if (result.getEventCode() != JobEvent.EVENT_CODE_OK) {
            throw new JobExecutionException(result);
        }
    }

    /**
     * This is the place to check job preconditions.
     * By default method is called from {@link #onPreExecute()}.
     * <br/>
     * {@link com.be.android.library.worker.exceptions.JobExecutionException}
     * will be thrown from {@link #onPreExecute()} if returned JobEvent specify any job status
     * different from {@link com.be.android.library.worker.base.JobEvent#EVENT_CODE_OK}.
     * <br/>
     * In such case returned event will be used as failure job result.
     *
     * @return {@link com.be.android.library.worker.base.JobEvent} with appropriate status code.
     * @see #onPreExecute()
     */
    protected JobEvent onCheckPreconditions() {
        return EVENT_OK;
    }

    protected void onExceptionCaughtBase(Exception e) {
        Log.e(LOG_TAG, String.format("exception caught while executing job '%s'", this));
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

    /**
     * Called after job has successfully executed.
     * <br />
     * If any exception is thrown then job result will change to failure.
     *
     * @param executionResult event returned from {@link #executeImpl()}
     *
     * @throws Exception
     */
    protected void onPostExecute(JobEvent executionResult) throws Exception {
        performPauseIfPaused();
    }

    @Override
    public JobStatusLock acquireStatusLock() {
        return null;
    }

    /**
     * Called after {@link #onPreExecute()}.
     * <br />
     * This is the place to implement your actual background job logic.
     * <br />
     * if any exception is thrown then job result will change to failure.
     * Also {@link #onExceptionCaught(Exception)} will be
     * called with thrown exception before job is finished.
     * <br />
     * If method has returned result, then {@link #onPostExecute(JobEvent)}
     * will be called before job is finished,
     *
     * @return result event
     * @throws Exception
     * @see #onPreExecute()
     * @see #onExceptionCaught(Exception)
     * @see #onPostExecute(JobEvent)
     */
    protected abstract JobEvent executeImpl() throws Exception;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseJob baseJob = (BaseJob) o;

        if (mIsCancelled != baseJob.mIsCancelled) return false;
        if (mExecutionHandler != null ? !mExecutionHandler.equals(baseJob.mExecutionHandler) : baseJob.mExecutionHandler != null)
            return false;
        if (mParams != null ? !mParams.equals(baseJob.mParams) : baseJob.mParams != null)
            return false;
        if (mPauseLatch != null ? !mPauseLatch.equals(baseJob.mPauseLatch) : baseJob.mPauseLatch != null)
            return false;
        if (mPauseLock != null ? !mPauseLock.equals(baseJob.mPauseLock) : baseJob.mPauseLock != null)
            return false;
        if (mStatusHolder.getJobStatus() != baseJob.mStatusHolder.getJobStatus()) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = mStatusHolder.getJobStatus().hashCode();
        result = 31 * result + (mIsCancelled ? 1 : 0);
        result = 31 * result + (mPauseLatch != null ? mPauseLatch.hashCode() : 0);
        result = 31 * result + (mPauseLock != null ? mPauseLock.hashCode() : 0);
        result = 31 * result + (mExecutionHandler != null ? mExecutionHandler.hashCode() : 0);
        result = 31 * result + (mParams != null ? mParams.hashCode() : 0);
        return result;
    }
}
