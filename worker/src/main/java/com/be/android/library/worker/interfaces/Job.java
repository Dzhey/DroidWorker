package com.be.android.library.worker.interfaces;

import com.be.android.library.worker.base.JobConfigurator;
import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.base.JobStatus;
import com.be.android.library.worker.base.JobStatusLock;
import com.be.android.library.worker.models.JobParams;
import com.be.android.library.worker.models.Params;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public interface Job extends Callable<JobEvent>, Runnable, JobEventObservable {

    int getJobId();

    boolean hasId();

    JobParams getParams();

    boolean hasParams();

    JobStatusLock acquireStatusLock();

    boolean isPending();

    boolean isFinished();

    boolean isFinishedOrCancelled();

    JobStatus getStatus();

    void cancel();

    boolean isCancelled();

    int pause();

    int unpause();

    int getPauseCount();

    boolean isPaused();

    void unpauseAll();

    void reset();

    JobEvent execute();

    Future<JobEvent> getPendingResult();

    JobConfigurator setup();
}
