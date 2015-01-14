package com.be.android.library.worker.handlers;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.be.android.library.worker.base.HierarchyViewer;
import com.be.android.library.worker.base.InvocationHandler;
import com.be.android.library.worker.base.JobCancelInvocationHandlerProvider;
import com.be.android.library.worker.base.JobEventInvocationHandlerProvider;
import com.be.android.library.worker.base.JobFailureInvocationHandlerProvider;
import com.be.android.library.worker.base.JobResultInvocationHandlerProvider;
import com.be.android.library.worker.base.JobSuccessInvocationHandlerProvider;
import com.be.android.library.worker.controllers.JobManager;
import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.interfaces.Job;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class JobEventDispatcher implements JobEventHandlerInterface {

    public static final String LOG_TAG = JobEventDispatcher.class.getSimpleName();

    private static final String KEY_PENDING_JOBS = "JobEventHandler_pending_jobs";
    private static final String KEY_FINISH_LISTENER_TAG = "JobEventHandler_listener_tag";

    private final LinkedList<ListenerEntry> mListeners;
    private final Set<Integer> mPendingJobs;
    private final Handler mHandler;
    private final String mListenerTag;
    private final HierarchyViewer mHierarchyViewer;

    private final CachedJobEventListener mJobFinishedListener = new CachedJobEventListener() {

        @Override
        public void onJobEventImpl(final JobEvent event) {
            if (mPendingJobs.contains(event.getJobId())) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        handleJobEvent(event);
                    }
                });
            }
        }
    };

    public JobEventDispatcher(Context context) {
        this(context, context.getClass().getSimpleName());
    }

    public JobEventDispatcher(Context context, String listenerName) {
        if (TextUtils.isEmpty(listenerName)) {
            throw new IllegalArgumentException("listenerName may not be empty");
        }

        mListeners = new LinkedList<ListenerEntry>();
        mPendingJobs = new HashSet<Integer>();
        mHandler = new Handler(Looper.getMainLooper());
        mListenerTag = String.format("%s_%s_%d", listenerName,
                getClass().getSimpleName(), System.currentTimeMillis());
        mHierarchyViewer = new HierarchyViewer(context);
        mHierarchyViewer.registerInvocationHandlerProvider(new JobEventInvocationHandlerProvider());
        mHierarchyViewer.registerInvocationHandlerProvider(new JobResultInvocationHandlerProvider());
        mHierarchyViewer.registerInvocationHandlerProvider(new JobSuccessInvocationHandlerProvider());
        mHierarchyViewer.registerInvocationHandlerProvider(new JobFailureInvocationHandlerProvider());
        mHierarchyViewer.registerInvocationHandlerProvider(new JobCancelInvocationHandlerProvider());
    }

    /**
     * Register previously submitted job to be pending by this handler
     * @param job job to register
     * @return true if successfully registered; false if no assigned job found
     * or job is already finished
     */
    public boolean addPendingJob(Job job) {
        if (job == null || job.isFinished()) {
            return false;
        }

        addPendingJobImpl(job.getJobId());

        return true;
    }

    /**
     * Register previously submitted job to be pending by this handler
     * @param jobId job id to register
     * @return true if successfully registered; false if no assigned job found
     * or job is already finished
     */
    public boolean addPendingJob(int jobId) {
        Job job = JobManager.getInstance().findJob(jobId);

        return addPendingJob(job);
    }

    private void addPendingJobImpl(int jobId) {
        if (mPendingJobs.isEmpty()) {
            JobManager.getInstance().addJobEventListener(mListenerTag, mJobFinishedListener);
        }

        mPendingJobs.add(jobId);
    }

    public boolean isPending(int jobId) {
        return mPendingJobs.contains(jobId);
    }

    public boolean isPending(String... jobTags) {
        Job job = JobManager.getInstance().findJob(jobTags);

        if (job == null) return false;

        return mPendingJobs.contains(job.getJobId());
    }

    public boolean isPending(Collection<String> jobTags) {
        Job job = JobManager.getInstance().findJob(jobTags);

        if (job == null) return false;

        return mPendingJobs.contains(job.getJobId());
    }

    public void saveState(Bundle outState) {
        if (outState == null) {
            throw new IllegalArgumentException("outState is null");
        }

        int i = 0;
        int[] ids = new int[mPendingJobs.size()];
        for (int jobId : mPendingJobs) {
            ids[i] = jobId;
            i++;
        }

        outState.putIntArray(KEY_PENDING_JOBS, ids);
        outState.putString(KEY_FINISH_LISTENER_TAG, mListenerTag);
    }

    public void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState == null || savedInstanceState.containsKey(KEY_PENDING_JOBS) == false) {
            return;
        }

        int[] ids = savedInstanceState.getIntArray(KEY_PENDING_JOBS);
        if (ids != null) {
            mPendingJobs.clear();
            for (int id : ids) {
                addPendingJobImpl(id);
            }

            String cachedListenerTag = savedInstanceState.getString(KEY_FINISH_LISTENER_TAG);
            if (cachedListenerTag != null) {
                flushJobEvents(cachedListenerTag);
            }
        }
    }

    private void flushJobEvents(String cachedListenerTag) {
        final CachedJobEventListener eventListener = (CachedJobEventListener)
                JobManager.getInstance().findJobEventListener(cachedListenerTag);

        if (eventListener == null) return;

        for (int jobId : mPendingJobs) {
            final JobEvent event = eventListener.getLastJobEvent(jobId);
            if (event != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        handleJobEvent(event);
                    }
                });
            }
        }
    }

    public int submitJob(Job job) {
        int jobId = JobManager.getInstance().submitJob(job);
        addPendingJobImpl(jobId);

        return jobId;
    }

    public void register(Object listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener should not be null");
        }

        for (ListenerEntry entry : mListeners) {
            Object ref = entry.getListenerObjectRef().get();
            if (ref != null && ref == listener) {
                Log.w(LOG_TAG, String.format("detected attempt to subscribe event " +
                        "listener multiple times ('%s')", listener));

                return;
            }
        }

        List<InvocationHandler> registry = mHierarchyViewer.fetchInvocationHandlers(listener.getClass());
        ListenerEntry entry = new ListenerEntry(new WeakReference<Object>(listener), registry);

        mListeners.add(entry);
    }

    public void unregister(Object listener) {
        if (listener == null) return;

        Iterator<ListenerEntry> iter = mListeners.iterator();
        while (iter.hasNext()) {
            ListenerEntry entry = iter.next();
            Object ref = entry.getListenerObjectRef().get();
            if (ref == null) {
                iter.remove();
                continue;
            }

            if (ref.equals(listener)) {
                iter.remove();
                return;
            }
        }
    }

    private void handleJobEvent(JobEvent jobEvent) {
        final int jobId = jobEvent.getJobId();

        if (dispatchJobEvent(jobEvent)) {
            if (jobEvent.isJobFinished()) {
                mPendingJobs.remove(jobId);

                if (mPendingJobs.isEmpty()) {
                    JobManager.getInstance().removeJobEventListener(mJobFinishedListener);
                }
            }
            mJobFinishedListener.consumeEvent(jobId);
        }
    }

    private boolean dispatchJobEvent(JobEvent jobEvent) {
        if (mListeners.isEmpty()) return true;

        boolean isDispatched = false;
        Iterator<ListenerEntry> iter = mListeners.iterator();
        while (iter.hasNext()) {
            ListenerEntry entry = iter.next();
            Object listener = entry.getListenerObjectRef().get();

            if (listener == null) {
                iter.remove();
                continue;
            }

            isDispatched = sendJobEvent(listener,
                    entry.getInvocationHandlers(), jobEvent) || isDispatched;
        }

        return isDispatched;
    }

    private boolean sendJobEvent(Object listener, List<InvocationHandler> handlers, JobEvent event) {
        for (InvocationHandler handler : handlers) {
            try {
                if (handler.canApply(listener, event)) {
                    handler.apply(listener, event);
                } else {
                    continue;
                }

                return true;

            } catch (IllegalAccessException e) {
                throw new RuntimeException("unable to invoke job result event handler", e);

            } catch (Exception e) {
                throw new RuntimeException(String.format(
                        "job result event handler threw an exception '%s'",
                        e.toString()), e);
            }
        }

        return false;
    }
}
