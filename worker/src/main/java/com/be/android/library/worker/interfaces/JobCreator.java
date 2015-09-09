package com.be.android.library.worker.interfaces;

import com.be.android.library.worker.models.JobParams;

public interface JobCreator<T extends Job> {
    T createJob(JobParams params);
}
