package com.be.android.library.worker.controllers;

import android.util.Log;

import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.interfaces.JobEventListener;
import com.be.android.library.worker.models.JobListenerEntry;
import com.be.android.library.worker.interfaces.JobEventObservable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class JobEventObservableImpl implements JobEventObservable {

    public static final String LOG_TAG = JobEventObservableImpl.class.getSimpleName();

    private List<JobListenerEntry<JobEventListener>> mEventListeners;

    private ReadWriteLock mEventListenersLock;
    private AtomicBoolean mIsNotifyingEventListeners;
    private volatile long mThreadId;

    public JobEventObservableImpl() {
    }

    @Override
    public void removeJobEventListeners() {
        mEventListenersLock = null;
        mIsNotifyingEventListeners = null;
        mEventListeners.clear();
    }

    @Override
    public boolean hasJobEventListener(JobEventListener listener) {
        if (listener == null || mEventListenersLock == null) return false;

        Lock lock = mEventListenersLock.readLock();
        lock.lock();
        try {

            if (mEventListenersLock == null) return false;

            for (JobListenerEntry<JobEventListener> entry : mEventListeners) {
                if (listener.equals(entry.getListenerReference().get())) {
                    return true;
                }
            }

        } finally {
            lock.unlock();
        }

        return false;
    }

    @Override
    public boolean hasJobEventListener(String listenerTag) {
        if (listenerTag == null || mEventListenersLock == null) return false;

        return findJobEventListener(listenerTag) != null;
    }

    @Override
    public JobEventListener findJobEventListener(String listenerTag) {
        if (listenerTag == null || mEventListenersLock == null) return null;

        Lock lock = mEventListenersLock.readLock();
        lock.lock();
        try {

            if (mEventListenersLock == null) return null;

            for (JobListenerEntry<JobEventListener> entry : mEventListeners) {
                if (listenerTag.equals(entry.getListenerTag())) {
                    return entry.getListenerReference().get();
                }
            }

        } finally {
            lock.unlock();
        }

        return null;
    }

    @Override
    public void addJobEventListener(JobEventListener listener) {
        addJobEventListener(null, listener);
    }

    @Override
    public void addJobEventListener(String listenerTag, JobEventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("cant add null finish listener");
        }

        if (mEventListeners == null) {
            synchronized (this) {
                if (mEventListeners == null) {
                    mEventListeners = new ArrayList<JobListenerEntry<JobEventListener>>(1);
                    mEventListenersLock = new ReentrantReadWriteLock(false);
                    mIsNotifyingEventListeners = new AtomicBoolean(false);
                }
            }
        }

        Lock lock = mEventListenersLock.writeLock();
        if (lock.tryLock() == false) {
            if (Thread.currentThread().getId() == mThreadId && mIsNotifyingEventListeners.get()) {
                throw new IllegalStateException("addJobEventListener() called while job " +
                        "is notifying it's listeners; don't try to add or remove job event " +
                        "listeners from listener's callback");
            } else {
                lock.lock();
            }
        }

        try {
            mEventListeners.add(new JobListenerEntry(listener, listenerTag));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean removeJobEventListener(String tag) {
        if (tag == null) {
            Log.w(LOG_TAG, "unable to remove listener for given null tag");
            return false;
        }

        return removeJobEventListener(tag, null);
    }

    @Override
    public boolean removeJobEventListener(JobEventListener listener) {
        if (listener == null) {
            Log.w(LOG_TAG, "unable to remove listener for given null listener");
            return false;
        }

        return removeJobEventListener(null, listener);
    }

    private boolean removeJobEventListener(String listenerTag, JobEventListener listener) {
        if (mEventListeners == null) return false;

        if (listenerTag == null && listener == null) {
            throw new IllegalArgumentException("unable to remove finish listener: " +
                    "neither tag not listener is provided to compare");
        }

        Lock lock = mEventListenersLock.writeLock();
        if (lock.tryLock() == false) {
            if (Thread.currentThread().getId() == mThreadId && mIsNotifyingEventListeners.get()) {
                throw new IllegalStateException("removeJobEventListener() called while job " +
                        "is notifying it's listeners; don't try to add or remove job event " +
                        "listeners from listener's callback");
            } else {
                lock.lock();
            }
        }

        try {
            Iterator<JobListenerEntry<JobEventListener>> iter = mEventListeners.iterator();
            while (iter.hasNext()) {
                JobListenerEntry entry = iter.next();
                WeakReference<JobEventListener> ref = entry.getListenerReference();
                JobEventListener item = ref.get();

                if (item == null) {
                    iter.remove();

                } else if (listener != null && listener.equals(item)) {
                    iter.remove();
                    return true;

                } else if (listenerTag != null && listenerTag.equals(entry.getListenerTag())) {
                    iter.remove();
                    return true;
                }
            }
        } finally {
            lock.unlock();
        }

        return false;
    }

    @Override
    public void notifyJobEvent(JobEvent event) {
        checkEventPreconditions(event);

        if (mEventListeners == null) return;

        Lock lock = mEventListenersLock.readLock();
        lock.lock();
        mIsNotifyingEventListeners.set(true);
        mThreadId = Thread.currentThread().getId();

        try {
            for (JobListenerEntry<JobEventListener> entry : mEventListeners) {
                JobEventListener listener = entry.getListenerReference().get();

                if (listener != null) {
                    listener.onJobEvent(event);
                }
            }

        } catch (Exception e) {
            Log.e(LOG_TAG, String.format("error while notifying job finish listeners; " +
                    "message: '%s'", e.getMessage()));
            e.printStackTrace();

        } finally {
            mIsNotifyingEventListeners.set(false);
            lock.unlock();
        }
    }

    private void checkEventPreconditions(JobEvent event) {
        if (event.getJobId() == JobManager.JOB_ID_UNSPECIFIED) {
            throw new IllegalArgumentException("can't post job event: job id is not specified");
        }

        if (event.getJobStatus() == null) {
            throw new IllegalArgumentException("can't post job event: job status is not specified");
        }

        if (event.getJob() == null) {
            throw new IllegalArgumentException("can't post job event: job is not specified");
        }
    }
}
