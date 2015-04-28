package com.be.android.library.worker.base;

import android.os.SystemClock;

import com.be.android.library.worker.interfaces.JobEventListener;
import com.be.android.library.worker.models.Params;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Future;

public class ProfilerJob extends BaseJob {

    private static final SimpleDateFormat TIMESTAMP_FORMAT =
            new SimpleDateFormat("hh:mm:ss:S");

    private BaseJob mWrappedJob;
    private long mPreExecuteStartTimeMillis;
    private long mPreExecuteEndTimeMillis;
    private long mPostExecuteStartTimeMillis;
    private long mPostExecuteEndTimeMillis;
    private long mExecuteImplStartTimeMillis;
    private long mExecuteImplEndTimeMillis;
    private long mTotalExecuteStartTimeMillis;
    private long mTotalExecuteEndTimeMillis;

    private final ExecutionHandler mExecutionHandler = new ExecutionHandler() {
        @Override
        public void onPreExecute() throws Exception {
            ProfilerJob.this.onPreExecuteBase();
        }

        @Override
        public JobEvent execute() {
            return ProfilerJob.this.execute();
        }

        @Override
        public void onPostExecute(JobEvent executionResult) throws Exception {
            ProfilerJob.this.onPostExecute(executionResult);
        }

        @Override
        public void onExceptionCaughtBase(Exception e) {
            ProfilerJob.this.onExceptionCaughtBase(e);
        }

        @Override
        public JobEvent executeImpl() throws Exception {
            return ProfilerJob.this.executeImpl();
        }
    };

    public static ProfilerJob create(BaseJob job) {
        return new ProfilerJob(job);
    }

    protected ProfilerJob(BaseJob wrappedJob) {
        mWrappedJob = wrappedJob;
        mWrappedJob.setExecutionHandler(mExecutionHandler);
    }

    @Override
    protected ExecutionHandler getExecutionHandler() {
        return mWrappedJob.getExecutionHandler();
    }

    @Override
    protected void setExecutionHandler(ExecutionHandler mExecutionHandler) {
        throw new UnsupportedOperationException();
    }

    public BaseJob getWrappedJob() {
        return mWrappedJob;
    }

    public long getPreExecuteStartTimeMillis() {
        return mPreExecuteStartTimeMillis;
    }

    public long getPreExecuteEndTimeMillis() {
        return mPreExecuteEndTimeMillis;
    }

    public long getPostExecuteStartTimeMillis() {
        return mPostExecuteStartTimeMillis;
    }

    public long getPostExecuteEndTimeMillis() {
        return mPostExecuteEndTimeMillis;
    }

    public long getExecuteImplStartTimeMillis() {
        return mExecuteImplStartTimeMillis;
    }

    public long getExecuteImplEndTimeMillis() {
        return mExecuteImplEndTimeMillis;
    }

    public long getTotalExecuteStartTimeMillis() {
        return mTotalExecuteStartTimeMillis;
    }

    public long getTotalExecuteEndTimeMillis() {
        return mTotalExecuteEndTimeMillis;
    }

    public String dumpProfile() {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("execute(); %s\n", formatDumpLine(
                getTotalExecuteStartTimeMillis(), getTotalExecuteEndTimeMillis())));

        sb.append(String.format("onPreExecuteBase(); %s\n", formatDumpLine(
                getPreExecuteStartTimeMillis(), getPreExecuteEndTimeMillis())));

        sb.append(String.format("executeImpl(); %s\n", formatDumpLine(
                getExecuteImplStartTimeMillis(), getExecuteImplEndTimeMillis())));

        sb.append(String.format("onPostExecute(); %s\n", formatDumpLine(
                getPostExecuteStartTimeMillis(), getPostExecuteEndTimeMillis())));

        return sb.toString();
    }

    private String formatDumpLine(long start, long end) {
        return String.format("start: %s; end: %s; duration: %dms",
                TIMESTAMP_FORMAT.format(new Date(start)),
                TIMESTAMP_FORMAT.format(new Date(end)),
                end - start
        );
    }

    @Override
    public JobStatusLock acquireStatusLock() {
        return mWrappedJob.acquireStatusLock();
    }

    @Override
    protected void onPreExecute() throws Exception {
        final long startRealtimeMillis = SystemClock.elapsedRealtime();

        mPreExecuteStartTimeMillis = System.currentTimeMillis();

        try {
            super.onPreExecuteBase();

        } finally {
            mPreExecuteEndTimeMillis = mPreExecuteStartTimeMillis
                    + SystemClock.elapsedRealtime() - startRealtimeMillis;
        }
    }

    @Override
    protected void onPostExecute(JobEvent executionResult) throws Exception {
        final long startRealtimeMillis = SystemClock.elapsedRealtime();

        mPostExecuteStartTimeMillis = System.currentTimeMillis();

        try {
            super.onPostExecute(executionResult);

        } finally {
            mPostExecuteEndTimeMillis = mPostExecuteStartTimeMillis
                    + SystemClock.elapsedRealtime() - startRealtimeMillis;
        }
    }

    @Override
    public JobEvent execute() {
        final long startRealtimeMillis = SystemClock.elapsedRealtime();
        mTotalExecuteStartTimeMillis = System.currentTimeMillis();

        try {
            return mWrappedJob.execute();

        } finally {
            mTotalExecuteEndTimeMillis = mTotalExecuteStartTimeMillis
                    + SystemClock.elapsedRealtime() - startRealtimeMillis;
        }
    }

    @Override
    protected JobEvent executeImpl() throws Exception {
        final long startRealtimeMillis = SystemClock.elapsedRealtime();
        mExecuteImplStartTimeMillis = System.currentTimeMillis();

        try {
            return mWrappedJob.executeImpl();

        } finally {
            mExecuteImplEndTimeMillis = mExecuteImplStartTimeMillis
                    + SystemClock.elapsedRealtime() - startRealtimeMillis;
        }
    }

    @Override
    public JobConfigurator createConfigurator() {
        return mWrappedJob.createConfigurator();
    }

    @Override
    public Params getParams() {
        return mWrappedJob.getParams();
    }

    @Override
    public void setParams(Params params) {
        mWrappedJob.setParams(params);
    }

    @Override
    public boolean isPending() {
        return mWrappedJob.isPending();
    }

    @Override
    public boolean isFinished() {
        return mWrappedJob.isFinished();
    }

    @Override
    public boolean isFinishedOrCancelled() {
        return mWrappedJob.isFinishedOrCancelled();
    }

    @Override
    public Future<JobEvent> getPendingResult() {
        return mWrappedJob.getPendingResult();
    }

    @Override
    public int pause() {
        return mWrappedJob.pause();
    }

    @Override
    public int getPauseCount() {
        return mWrappedJob.getPauseCount();
    }

    @Override
    public int unpause() {
        return mWrappedJob.unpause();
    }

    @Override
    public void unpauseAll() {
        mWrappedJob.unpauseAll();
    }

    @Override
    public void cancel() {
        mWrappedJob.cancel();
    }

    @Override
    public boolean isCancelled() {
        return mWrappedJob.isCancelled();
    }

    @Override
    protected void onReset() {
        mWrappedJob.reset();
    }

    @Override
    public boolean isPaused() {
        return mWrappedJob.isPaused();
    }

    @Override
    public JobEvent call() throws Exception {
        return mWrappedJob.execute();
    }

    @Override
    public void run() {
        mWrappedJob.run();
    }

    @Override
    public boolean hasJobEventListener(JobEventListener listener) {
        return mWrappedJob.hasJobEventListener(listener);
    }

    @Override
    public boolean hasJobEventListener(String listenerTag) {
        return mWrappedJob.hasJobEventListener(listenerTag);
    }

    @Override
    public JobEventListener findJobEventListener(String listenerTag) {
        return mWrappedJob.findJobEventListener(listenerTag);
    }

    @Override
    public void addJobEventListener(JobEventListener listener) {
        mWrappedJob.addJobEventListener(listener);
    }

    @Override
    public void addJobEventListener(String tag, JobEventListener listener) {
        mWrappedJob.addJobEventListener(tag, listener);
    }

    @Override
    public boolean removeJobEventListener(JobEventListener listener) {
        return mWrappedJob.removeJobEventListener(listener);
    }

    @Override
    public boolean removeJobEventListener(String tag) {
        return mWrappedJob.removeJobEventListener(tag);
    }

    @Override
    protected void notifyJobEventImpl(JobEvent event) {
        super.notifyJobEventImpl(event);
    }

    @Override
    public void removeJobEventListeners() {
        mWrappedJob.removeJobEventListeners();
    }
}
