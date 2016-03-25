package com.be.library.worker.annotations.compiler;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import java.util.Collection;
import java.util.Map;

public class JobClassInfo {

    private final Multimap<String, JobExtraClassInfo> mJobInfo;

    public JobClassInfo() {
        mJobInfo = Multimaps.newListMultimap(Maps.newHashMap(), Lists::newArrayList);
    }

    public void registerJobExtraInfo(JobExtraClassInfo info) {
        mJobInfo.put(info.getQualifiedJobName(), info);
    }

    public Map<String, Collection<JobExtraClassInfo>> getClassesInfo() {
        return Multimaps.asMap(mJobInfo);
    }
}
