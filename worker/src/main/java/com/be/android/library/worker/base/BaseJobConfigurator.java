package com.be.android.library.worker.base;

import com.be.android.library.worker.interfaces.ParamsBuilder;
import com.be.android.library.worker.models.Flags;
import com.be.android.library.worker.models.Params;

import java.util.Collection;
import java.util.Map;

public class BaseJobConfigurator implements JobConfigurator {

    private final BaseJob mJob;
    private final Flags.OnFlagSetListener mOnFlagSetListener = new Flags.OnFlagSetListener() {
        @Override
        public void onFlagSet(Flags flags, final String flag, final boolean newValue, boolean hasChanged) {
            if (!hasChanged) {
                return;
            }

            final JobEvent event = new JobEvent.Builder()
                    .eventCode(JobEvent.EVENT_CODE_UPDATE)
                    .extraCode(JobEvent.EXTRA_CODE_FLAG_STATUS_CHANGED)
                    .payload(new Map.Entry<String,Boolean>() {
                        @Override
                        public String getKey() {
                            return flag;
                        }

                        @Override
                        public Boolean getValue() {
                            return newValue;
                        }

                        @Override
                        public Boolean setValue(Boolean aBoolean) {
                            throw new UnsupportedOperationException();
                        }
                    })
                    .build();
            mJob.notifyJobEvent(event);
        }
    };

    private ParamsBuilder mParamsBuilder;
    private Params mParams;

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
    public Params build() {
        checkInitialized();

        mParams = mParamsBuilder.build();
        mParams.getFlags().addOnFlagSetListener(mOnFlagSetListener);
        mParams.setJobClassName(mJob.getClass().getName());

        return mParams;
    }

    @Override
    public void apply() {
        if (mParams == null) {
            mParams = mParamsBuilder.build();
            mParams.getFlags().addOnFlagSetListener(mOnFlagSetListener);
            mParams.setJobClassName(mJob.getClass().getName());
        }

        mJob.setParams(mParams);
    }

    private void checkInitialized() {
        if (mParamsBuilder == null) {
            throw new IllegalStateException("configurator is not initialized");
        }
    }
}
