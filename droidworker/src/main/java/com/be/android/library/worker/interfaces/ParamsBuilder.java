package com.be.android.library.worker.interfaces;

import com.be.android.library.worker.models.JobParams;

import java.util.Collection;
import java.util.Map;

public interface ParamsBuilder {
    ParamsBuilder group(int groupId);

    ParamsBuilder priority(int priority);

    ParamsBuilder payload(Object payload);

    ParamsBuilder tags(String... tags);

    ParamsBuilder tags(Collection<String> tags);

    ParamsBuilder addTag(String tag);

    ParamsBuilder removeTag(String tag);

    ParamsBuilder addExtra(String key, Object value);

    <T extends Map<String, Object>> ParamsBuilder addExtras(T extras);

    ParamsBuilder removeExtra(String key);

    ParamsBuilder flag(String flag, boolean value);

    ParamsBuilder flag(String flag);

    ParamsBuilder flags(String... flags);

    /**
     * Set job class name
     * <br>
     * Similar to {@link #addExtra(String, Object)} with {@link JobParams#EXTRA_JOB_TYPE}
     * as first argument
     * @param jobClazz Job class
     * @return this object
     */
    ParamsBuilder jobClass(Class<? extends Job> jobClazz);

    JobParams build();
}
