package com.be.library.worker.annotations.compiler;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JobClassInfo {

    private final Multimap<String, JobExtraClassInfo> mJobInfo;

    public JobClassInfo() {
        mJobInfo = Multimaps.newListMultimap(
                new HashMap<String, Collection<JobExtraClassInfo>>(),
                new Supplier<List<JobExtraClassInfo>>() {
                    @Override
                    public List<JobExtraClassInfo> get() {
                        return Lists.newArrayList();
                    }
                });
    }

    public void registerJobExtraInfo(JobExtraClassInfo info) {
        mJobInfo.put(info.getQualifiedJobName(), info);
    }

    public Map<String, Collection<JobExtraClassInfo>> getClassesInfo() {
        return Multimaps.asMap(mJobInfo);
    }
}
