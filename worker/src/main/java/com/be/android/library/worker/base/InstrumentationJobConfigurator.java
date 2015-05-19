package com.be.android.library.worker.base;

import com.be.android.library.worker.interfaces.ParamsBuilder;
import com.be.android.library.worker.models.Params;

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
            public JobEvent executeImpl() throws Exception {
                if (mInstrumentationHandler != null) {
                    mInstrumentationHandler.executeImpl();
                }

                return mJobExecutionHandler.executeImpl();
            }
        };

        mJob.setExecutionHandler(mProxyExecutionHandler);
    }

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
    public void apply() {
        mJobConfigurator.apply();
    }

    @Override
    public ParamsBuilder group(int groupId) {
        return mJobConfigurator.group(groupId);
    }

    @Override
    public ParamsBuilder priority(int priority) {
        return mJobConfigurator.priority(priority);
    }

    @Override
    public ParamsBuilder payload(Object payload) {
        return mJobConfigurator.payload(payload);
    }

    @Override
    public ParamsBuilder tags(String... tags) {
        return mJobConfigurator.tags(tags);
    }

    @Override
    public ParamsBuilder tags(Collection<String> tags) {
        return mJobConfigurator.tags(tags);
    }

    @Override
    public ParamsBuilder addTag(String tag) {
        return mJobConfigurator.addTag(tag);
    }

    @Override
    public ParamsBuilder removeTag(String tag) {
        return mJobConfigurator.removeTag(tag);
    }

    @Override
    public ParamsBuilder addExtra(String key, Object value) {
        return mJobConfigurator.addExtra(key, value);
    }

    @Override
    public ParamsBuilder removeExtra(String key) {
        return mJobConfigurator.removeExtra(key);
    }

    @Override
    public ParamsBuilder flag(String flag, boolean value) {
        return mJobConfigurator.flag(flag, value);
    }

    @Override
    public Params build() {
        return mJobConfigurator.build();
    }
}
