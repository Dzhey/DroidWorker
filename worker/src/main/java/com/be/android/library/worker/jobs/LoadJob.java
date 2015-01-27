package com.be.android.library.worker.jobs;


import com.be.android.library.worker.base.ForkJoinJob;
import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.models.LoadJobResult;

public abstract class LoadJob extends ForkJoinJob {

    @Override
    protected final JobEvent executeImpl() {
        return performLoad();
    }

    protected abstract LoadJobResult<?> performLoad();
}
