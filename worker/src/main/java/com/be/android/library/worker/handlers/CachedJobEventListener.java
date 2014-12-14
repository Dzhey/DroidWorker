package com.be.android.library.worker.handlers;

import android.util.Log;
import android.util.SparseArray;

import com.be.android.library.worker.interfaces.JobEventListener;
import com.be.android.library.worker.base.JobEvent;

public abstract class CachedJobEventListener implements JobEventListener {

    private static final String LOG_TAG = CachedJobEventListener.class.getSimpleName();
    private static final int WARNING_SIZE_THRESHOLD = 50;

    /**
     * Each job event mapped to job id
     */
    private final SparseArray<JobEvent> mLastJobEvents;

    public CachedJobEventListener() {
        mLastJobEvents = new SparseArray<JobEvent>(1);
    }

    @Override
    public final void onJobEvent(JobEvent event) {
        mLastJobEvents.put(event.getJobId(), event);

        if (mLastJobEvents.size() > WARNING_SIZE_THRESHOLD) {
            Log.e(LOG_TAG, String.format("'%s' is overloaded " +
                    "of job events; make sure to call consumeEvent()", getClass().getSimpleName()));
        }

        onJobEventImpl(event);
    }

    public JobEvent getLastJobEvent(int jobId) {
        return mLastJobEvents.get(jobId);
    }

    public void clearCachedEvents() {
        mLastJobEvents.clear();
    }

    public void consumeEvent(int jobId) {
        mLastJobEvents.remove(jobId);
    }

    public abstract void onJobEventImpl(JobEvent event);
}
