package com.be.android.library.worker.util;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.base.JobStatus;
import com.be.android.library.worker.controllers.JobManager;
import com.be.android.library.worker.interfaces.Job;
import com.be.android.library.worker.interfaces.JobEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by dzhey on 29/09/15.
 */
public class JobProgressTracker {

    private static final String EXTRA_STATE_REGISTERED_JOBS = "progress_tracker_registered_jobs";

    public interface Callbacks {
        void onProgressStarted();
        void onProgressStopped();
    }

    public static class SimpleCallbacks implements Callbacks {
        @Override
        public void onProgressStarted() {
        }

        @Override
        public void onProgressStopped() {
        }
    }

    private final Handler mHandler;
    private final JobManager mJobManager;
    private final List<JobSelector> mSelectors;
    private final Set<Integer> mRegisteredJobs;
    private Callbacks mCallbacks;
    private boolean mIsInProgress;

    private final Callbacks mSafeCallbacks = new Callbacks() {
        @Override
        public void onProgressStarted() {
            if (mCallbacks != null) {
                mCallbacks.onProgressStarted();
            }
        }

        @Override
        public void onProgressStopped() {
            if (mCallbacks != null) {
                mCallbacks.onProgressStopped();
            }
        }
    };

    private final JobEventListener mJobEventListener = new JobEventListener() {
        @Override
        public void onJobEvent(final JobEvent event) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    handleJobEvent(event);
                }
            });
        }
    };

    public JobProgressTracker(JobManager jobManager, Handler handler) {
        mJobManager = jobManager;
        mRegisteredJobs = new HashSet<Integer>();
        mSelectors = new ArrayList<JobSelector>();
        mHandler = handler;
    }

    public JobProgressTracker(JobManager jobManager) {
        this(jobManager, new Handler(Looper.getMainLooper()));
    }

    public void saveInstanceState(Bundle outState) {
        final int[] ids = new int[mRegisteredJobs.size()];

        int i = 0;
        for (int id : mRegisteredJobs) {
            ids[i] = id;
            i++;
        }

        outState.putIntArray(EXTRA_STATE_REGISTERED_JOBS, ids);
    }

    public void restoreInstanceState(Bundle state) {
        if (state.containsKey(EXTRA_STATE_REGISTERED_JOBS)) {
            final int[] ids = state.getIntArray(EXTRA_STATE_REGISTERED_JOBS);
            mRegisteredJobs.clear();
            for (int id : ids) {
                mRegisteredJobs.add(id);
            }
        }
    }

    public void triggerUpdate() {
        updateProgressStatus();
    }

    public void attach() {
        mJobManager.removeJobEventListener(mJobEventListener);
        mJobManager.addJobEventListener(mJobEventListener);
        updateProgressStatus();
    }

    public void detach() {
        mJobManager.removeJobEventListener(mJobEventListener);
    }

    public void addSelector(JobSelector selector) {
        mSelectors.add(selector);
    }

    public void removeSelector(JobSelector selector) {
        mSelectors.remove(selector);
    }

    public List<JobSelector> getSelectors() {
        return Collections.unmodifiableList(mSelectors);
    }

    public void registerJob(int jobId) {
        mRegisteredJobs.add(jobId);
        updateProgressStatus();
    }

    public void unregisterJob(int jobId) {
        mRegisteredJobs.remove(jobId);
        updateProgressStatus();
    }

    public Set<Integer> getRegisteredJobs() {
        return Collections.unmodifiableSet(mRegisteredJobs);
    }

    public Callbacks getCallbacks() {
        return mCallbacks;
    }

    public void setCallbacks(Callbacks callbacks) {
        mCallbacks = callbacks;
    }

    public boolean isInProgress() {
        return mIsInProgress;
    }

    protected void handleJobEvent(JobEvent event) {
        updateProgressStatus();

        if (event.isJobFinished()) {
            mRegisteredJobs.remove(event.getJobParams().getJobId());
        }
    }

    private void updateProgressStatus() {
        final boolean isInProgress = mIsInProgress;

        if (mRegisteredJobs.isEmpty()) {
            mIsInProgress = false;

            if (isInProgress) {
                mSafeCallbacks.onProgressStopped();
            }

            return;
        }

        for (JobSelector selector : mSelectors) {
            final List<Job> jobs = mJobManager.findAll(selector);

            if (jobs.isEmpty()) {
                if (isInProgress) {
                    mIsInProgress = false;
                    mSafeCallbacks.onProgressStopped();
                    return;
                }
            }

            for (Job job : jobs) {
                final JobStatus status = job.getStatus();
                if (status == JobStatus.PENDING || status == JobStatus.IN_PROGRESS) {
                    if (!isInProgress) {
                        mIsInProgress = true;
                        mSafeCallbacks.onProgressStarted();
                        return;
                    }
                } else {
                    if (isInProgress) {
                        mIsInProgress = false;
                        mSafeCallbacks.onProgressStopped();
                        return;
                    }
                }
            }
        }
    }
}
