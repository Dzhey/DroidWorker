package com.be.android.library.worker.base;

import com.be.android.library.worker.interfaces.Job;
import com.be.android.library.worker.interfaces.ParamsBuilder;
import com.be.android.library.worker.models.JobParams;

import java.util.Collection;
import java.util.Map;

public interface JobConfigurator extends ParamsBuilder {
    JobConfigurator group(int groupId);
    JobConfigurator priority(int priority);
    JobConfigurator payload(Object payload);
    JobConfigurator tags(String... tags);
    JobConfigurator tags(Collection<String> tags);
    JobConfigurator addTag(String tag);
    JobConfigurator removeTag(String tag);
    JobConfigurator addExtra(String key, Object value);
    <T extends Map<String, Object>> JobConfigurator addExtras(T extras);
    JobConfigurator removeExtra(String key);
    JobConfigurator flag(String flag, boolean value);
    JobConfigurator flag(String flag);
    JobConfigurator params(JobParams params);
    Job apply();
    Job getJob();
}
