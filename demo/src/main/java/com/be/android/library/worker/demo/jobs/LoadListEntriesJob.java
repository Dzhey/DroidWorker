package com.be.android.library.worker.demo.jobs;

import com.be.android.library.worker.base.ForkJoinJob;
import com.be.android.library.worker.controllers.JobManager;
import com.be.android.library.worker.demo.model.MultiloadDemoEntry;
import com.be.android.library.worker.exceptions.JobExecutionException;
import com.be.android.library.worker.models.LoadJobResult;
import com.be.library.worker.annotations.Inherited;
import com.be.library.worker.annotations.JobFlag;

import java.util.ArrayList;
import java.util.List;

@Inherited(LoadListEntryJob.class)
public class LoadListEntriesJob extends ForkJoinJob {

    @Inherited
    String mImageUrl;

    @JobFlag
    boolean mUseDelay = true;

    @Override
    protected LoadJobResult<List<MultiloadDemoEntry>> executeImpl() throws Exception {
        final List<ForkJoiner> mJoiners = new ArrayList<ForkJoiner>();

        for (int i = 0; i < 20; i++) {
            mJoiners.add(loadListEntryAsync(i));
        }

        final List<MultiloadDemoEntry> results = new ArrayList<MultiloadDemoEntry>();
        for (ForkJoiner joiner : mJoiners) {
            LoadJobResult<MultiloadDemoEntry> result = (LoadJobResult<MultiloadDemoEntry>) joiner.join();
            results.add(result.getData());
        }

        return new LoadJobResult<List<MultiloadDemoEntry>>(results);
    }

    private ForkJoiner loadListEntryAsync(int entryId) throws JobExecutionException {
        final LoadListEntryJob job = new LoadListEntryJob(entryId);

        job.setup().group(JobManager.JOB_GROUP_UNIQUE).apply();

        return forkJob(job);
    }
}
