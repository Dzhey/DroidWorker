package com.be.android.library.worker.interfaces;

import com.be.android.library.worker.models.Params;

import java.util.Collection;

public interface ParamsBuilder {
    ParamsBuilder group(int groupId);

    ParamsBuilder priority(int priority);

    ParamsBuilder payload(Object payload);

    ParamsBuilder tags(String... tags);

    ParamsBuilder tags(Collection<String> tags);

    ParamsBuilder addTag(String tag);

    ParamsBuilder removeTag(String tag);

    ParamsBuilder addExtra(String key, Object value);

    ParamsBuilder removeExtra(String key);

    ParamsBuilder flag(String flag, boolean value);

    Params build();
}
