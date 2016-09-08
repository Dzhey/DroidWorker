package com.be.library.worker.rxbindings;

import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.interfaces.JobEventListener;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Eugene Byzov gdzhey@gmail.com
 *         Created on 09-Sep-16.
 */
class CompositeEventListener implements JobEventListener {

    private final Set<Integer> mPendingJobs;
    private JobEventListener mDelegate;

    CompositeEventListener(Collection<Integer> pendingJobs) {
        mPendingJobs = new HashSet<>(pendingJobs);
    }

    @Override
    public void onJobEvent(JobEvent event) {
        if (!mPendingJobs.contains(event.getJobId())) {
            return;
        }

        if (event.isJobFinished()) {
            mPendingJobs.remove(event.getJobId());
        }

        mDelegate.onJobEvent(event);
    }

    public boolean isCompleted() {
        return mPendingJobs.isEmpty();
    }

    public JobEventListener getDelegate() {
        return mDelegate;
    }

    public void setDelegate(JobEventListener delegate) {
        mDelegate = delegate;
    }
}
