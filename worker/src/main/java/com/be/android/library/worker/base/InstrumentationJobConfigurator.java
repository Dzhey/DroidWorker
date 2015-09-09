package com.be.android.library.worker.base;

import com.be.android.library.worker.interfaces.Job;
import com.be.android.library.worker.interfaces.ParamsBuilder;
import com.be.android.library.worker.models.JobParams;

import java.util.Collection;

public class InstrumentationJobConfigurator implements JobConfigurator {

    private final JobConfigurator mJobConfigurator;
    private final BaseJob.ExecutionHandler mProxyExecutionHandler;
    private BaseJob.ExecutionHandler mInstrumentationHandler;
    private BaseJob.ExecutionHandler mJobExecutionHandler;
    private final BaseJob mJob;

    public InstrumentationJobConfigurator(BaseJob job, JobConfigurator jobConfigurator) {
        mJobConfigurator = jobConfigurator;
        mJob = job;
        mJobExecutionHandler = mJob.getExecutionHandler();

        mProxyExecutionHandler = new BaseJob.ExecutionHandler() {
            @Override
            public JobEvent onCheckPreconditions() throws Exception {
                if (mInstrumentationHandler != null) {
                    mInstrumentationHandler.onCheckPreconditions();
                }

                return mJobExecutionHandler.onCheckPreconditions();
            }

            @Override
            public void onPreExecute() throws Exception {
                if (mInstrumentationHandler != null) {
                    mInstrumentationHandler.onPreExecute();
                }

                mJobExecutionHandler.onPreExecute();
            }

            @Override
            public JobEvent execute() {
                if (mInstrumentationHandler != null) {
                    mInstrumentationHandler.execute();
                }

                return mJobExecutionHandler.execute();
            }

            @Override
            public void onPostExecute(JobEvent executionResult) throws Exception {
                if (mInstrumentationHandler != null) {
                    mInstrumentationHandler.onPostExecute(executionResult);
                }

                mJobExecutionHandler.onPostExecute(executionResult);
            }

            @Override
            public void onExceptionCaught(Exception e) {
                if (mInstrumentationHandler != null) {
                    mInstrumentationHandler.onExceptionCaught(e);
                }

                mJobExecutionHandler.onExceptionCaught(e);
            }

            @Override
            public void onJobFinished(JobEvent executionResult) {
                if (mInstrumentationHandler != null) {
                    mInstrumentationHandler.onJobFinished(executionResult);
                }

                mJobExecutionHandler.onJobFinished(executionResult);
            }

            @Override
            public JobEvent executeImpl() throws Exception {
                if (mInstrumentationHandler != null) {
                    mInstrumentationHandler.executeImpl();
                }

                return mJobExecutionHandler.executeImpl();
            }
        };

        mJob.setExecutionHandler(mProxyExecutionHandler);
    }

    @Override
    public BaseJob getJob() {
        return mJob;
    }

    public BaseJob.ExecutionHandler getInstrumentationHandler() {
        return mInstrumentationHandler;
    }

    public void setInstrumentationHandler(BaseJob.ExecutionHandler handler) {
        mInstrumentationHandler = handler;
    }

    @Override
    public BaseJob apply() {
        mJobConfigurator.apply();

        return mJob;
    }

    @Override
    public InstrumentationJobConfigurator group(int groupId) {
        mJobConfigurator.group(groupId);

        return this;
    }

    @Override
    public InstrumentationJobConfigurator priority(int priority) {
        mJobConfigurator.priority(priority);

        return this;
    }

    @Override
    public InstrumentationJobConfigurator payload(Object payload) {
        mJobConfigurator.payload(payload);

        return this;
    }

    @Override
    public InstrumentationJobConfigurator tags(String... tags) {
        mJobConfigurator.tags(tags);

        return this;
    }

    @Override
    public InstrumentationJobConfigurator tags(Collection<String> tags) {
        mJobConfigurator.tags(tags);

        return this;
    }

    @Override
    public InstrumentationJobConfigurator addTag(String tag) {
        mJobConfigurator.addTag(tag);

        return this;
    }

    @Override
    public InstrumentationJobConfigurator removeTag(String tag) {
        mJobConfigurator.removeTag(tag);

        return this;
    }

    @Override
    public InstrumentationJobConfigurator addExtra(String key, Object value) {
        mJobConfigurator.addExtra(key, value);

        return this;
    }

    @Override
    public InstrumentationJobConfigurator removeExtra(String key) {
        mJobConfigurator.removeExtra(key);

        return this;
    }

    @Override
    public InstrumentationJobConfigurator flag(String flag, boolean value) {
        mJobConfigurator.flag(flag, value);

        return this;
    }

    @Override
    public ParamsBuilder flag(String flag) {
        mJobConfigurator.flag(flag);

        return this;
    }

    @Override
    public ParamsBuilder jobClass(Class<? extends Job> jobClazz) {
        mJobConfigurator.jobClass(jobClazz);

        return this;
    }

    @Override
    public JobConfigurator params(JobParams params) {
        mJobConfigurator.params(params);

        return this;
    }

    @Override
    public JobParams build() {
        return mJobConfigurator.build();
    }
}
