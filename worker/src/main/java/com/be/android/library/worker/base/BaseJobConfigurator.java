package com.be.android.library.worker.base;

import com.be.android.library.worker.interfaces.ParamsBuilder;
import com.be.android.library.worker.models.Flag;
import com.be.android.library.worker.models.FlagChangeEvent;
import com.be.android.library.worker.models.Flags;
import com.be.android.library.worker.models.JobParams;
import com.be.android.library.worker.models.Params;

import java.util.Collection;

public class BaseJobConfigurator implements JobConfigurator {

    private final BaseJob mJob;
    private final Flags.OnFlagSetListener mOnFlagSetListener = new Flags.OnFlagSetListener() {
        @Override
        public void onFlagSet(Flags flags, final String flag, final boolean newValue, boolean hasChanged) {
            if (!hasChanged) {
                return;
            }

            if (!mJob.hasId()) {
                return;
            }

            mJob.notifyJobEvent(new FlagChangeEvent(mJob.getStatus(), Flag.create(flag, newValue)));
        }
    };

    private ParamsBuilder mParamsBuilder;

    public BaseJobConfigurator(BaseJob job) {
        mJob = job;
    }

    final void init() {
        if (mParamsBuilder != null) {
            throw new IllegalStateException("already initialized");
        }

        mParamsBuilder = createBuilder();
    }

    protected ParamsBuilder createBuilder() {
        return Params.create();
    }

    @Override
    public JobConfigurator group(int groupId) {
        checkInitialized();

        mParamsBuilder.group(groupId);

        return this;
    }

    @Override
    public JobConfigurator priority(int priority) {
        checkInitialized();

        mParamsBuilder.priority(priority);

        return this;
    }

    @Override
    public JobConfigurator payload(Object payload) {
        checkInitialized();

        mParamsBuilder.payload(payload);

        return this;
    }

    @Override
    public JobConfigurator tags(String... tags) {
        checkInitialized();

        mParamsBuilder.tags(tags);

        return this;
    }

    @Override
    public JobConfigurator tags(Collection<String> tags) {
        checkInitialized();

        mParamsBuilder.tags(tags);

        return this;
    }

    @Override
    public JobConfigurator addTag(String tag) {
        checkInitialized();

        mParamsBuilder.addTag(tag);

        return this;
    }

    @Override
    public JobConfigurator removeTag(String tag) {
        checkInitialized();

        mParamsBuilder.removeTag(tag);

        return this;
    }

    @Override
    public JobConfigurator addExtra(String key, Object value) {
        checkInitialized();

        mParamsBuilder.addExtra(key, value);

        return this;
    }

    @Override
    public JobConfigurator removeExtra(String key) {
        checkInitialized();

        mParamsBuilder.removeExtra(key);

        return this;
    }

    @Override
    public JobConfigurator flag(String flag, boolean value) {
        checkInitialized();

        mParamsBuilder.flag(flag, value);

        return this;
    }

    @Override
    public JobParams build() {
        checkInitialized();

        JobParams params = mParamsBuilder.build();
        params.getFlags().addOnFlagSetListener(mOnFlagSetListener);
        params.setJobClassName(mJob.getClass().getName());

        return params;
    }

    @Override
    public BaseJob apply() {
        if (!isInitialized()) {
            init();
        }

        JobParams params = mParamsBuilder.build();
        params.getFlags().addOnFlagSetListener(mOnFlagSetListener);
        params.setJobClassName(mJob.getClass().getName());

        mJob.setParams(params);

        return mJob;
    }

    @Override
    public BaseJob getJob() {
        return mJob;
    }

    private boolean isInitialized() {
        return mParamsBuilder != null;
    }

    private void checkInitialized() {
        if (!isInitialized()) {
            throw new IllegalStateException("configurator is not initialized");
        }
    }
}