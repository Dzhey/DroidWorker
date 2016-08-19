package com.be.android.library.worker.handlers;

import android.annotation.TargetApi;
import android.os.Build;

import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.interfaces.JobEventListener;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Job event listener used to retain job events for short period of time
 */
public abstract class CachedJobEventListener implements JobEventListener {

    private static final int MAX_SIZE_THRESHOLD = 10;

    private final LinkedList<JobEvent> mLastJobEvents;

    public CachedJobEventListener() {
        mLastJobEvents = new LinkedList<JobEvent>();
    }

    @Override
    public final void onJobEvent(JobEvent event) {
        if (mLastJobEvents.size() >= MAX_SIZE_THRESHOLD) {
            mLastJobEvents.removeFirst();
        }

        if (!onJobEventImpl(event)) {
            mLastJobEvents.addLast(event);
        }
    }

    public JobEvent getLastJobEvent(int jobId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return getLastJobEventApi9(jobId);
        }

        // API 8
        final int sz = mLastJobEvents.size();
        for (int i = sz - 1; i >= 0; i--) {
            JobEvent event = mLastJobEvents.get(i);

            if (event.getJobId() == jobId) {
                return event;
            }
        }

        return null;
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private JobEvent getLastJobEventApi9(int jobId) {
        final Iterator<JobEvent> iter = mLastJobEvents.descendingIterator();

        while (iter.hasNext()) {
            JobEvent event = iter.next();
            if (event.getJobId() == jobId) {
                return event;
            }
        }

        return null;
    }

    /**
     * Remove all events from cache
     */
    public void clearCachedEvents() {
        mLastJobEvents.clear();
    }

    /**
     * Remove event from cache
     *
     * @param event event to remove
     */
    public void consumeEvent(JobEvent event) {
        mLastJobEvents.remove(event);
    }

    /**
     * Implementation of handler
     *
     * @param event event to remove
     * @return true if the event should be removed from cache
     */
    public abstract boolean onJobEventImpl(JobEvent event);
}
