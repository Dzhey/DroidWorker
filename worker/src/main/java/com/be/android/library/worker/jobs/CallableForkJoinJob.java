package com.be.android.library.worker.jobs;

import android.util.Log;

import com.be.android.library.worker.base.ForkJoinJob;
import com.be.android.library.worker.base.JobEvent;

import java.util.concurrent.Callable;

public class CallableForkJoinJob extends ForkJoinJob {

    private static final String LOG_TAG = CallableForkJoinJob.class.getSimpleName();

    private final Callable<JobEvent> mCallable;

    public CallableForkJoinJob(int jobGroupId, Callable<JobEvent> callable) {
        mCallable = callable;
        setGroupId(jobGroupId);
    }

    @Override
    protected JobEvent executeImpl() {
        try {
            return mCallable.call();

        } catch (Exception e) {
            Log.e(LOG_TAG, String.format("error executing callable job; e: '%s'; " +
                            "callable: '%s'", e.toString(), mCallable));

            return JobEvent.failure();
        }
    }
}
