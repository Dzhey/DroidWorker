package com.be.android.library.worker.demo.jobs;

import com.be.android.library.worker.jobs.LoadJob;
import com.be.android.library.worker.models.LoadJobResult;

public class SimpleLoadResultJob extends LoadJob {

    @Override
    protected LoadJobResult<String> performLoad() throws Exception {

        Thread.sleep(4000);

        return new LoadJobResult<>("success!");
    }

}
