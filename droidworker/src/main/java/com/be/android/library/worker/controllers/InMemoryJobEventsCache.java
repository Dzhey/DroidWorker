package com.be.android.library.worker.controllers;

import android.os.SystemClock;
import android.util.Log;

import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.interfaces.CacheEntryDescriptor;
import com.be.android.library.worker.interfaces.JobEventsCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class InMemoryJobEventsCache implements JobEventsCache {

    private static final class CacheEntry {
        long accessTimeMillis;
        long timeoutMillis;
        JobEvent jobEvent;
        String key;
        int size;

        @Override
        public String toString() {
            return "CacheEntry{" +
                    "key='" + key + '\'' +
                    ", size=" + size +
                    '}';
        }
    }

    private static final String LOG_TAG = InMemoryJobEventsCache.class.getSimpleName();

    private final AtomicInteger mCacheSize;
    private final Map<String, CacheEntry> mCache;
    private final ReadWriteLock mCacheLock;
    private int mMaxCacheSize;
    private boolean mIsTraceEnabled;

    public InMemoryJobEventsCache(int maxCacheSize) {
        mCacheSize = new AtomicInteger(0);
        mCache = new HashMap<String, CacheEntry>();
        mCacheLock = new ReentrantReadWriteLock(false);
        mMaxCacheSize = maxCacheSize;
    }

    public boolean isTraceEnabled() {
        return mIsTraceEnabled;
    }

    public void setIsTraceEnabled(boolean isTraceEnabled) {
        this.mIsTraceEnabled = isTraceEnabled;
    }

    public int getMaxCacheSize() {
        return mMaxCacheSize;
    }

    public void setMaxCacheSize(int maxCacheSize) {
        if (maxCacheSize < 0) {
            throw new IllegalArgumentException("maxCacheSize < 0");
        }

        this.mMaxCacheSize = maxCacheSize;
    }

    @Override
    public void evictAll() {
        final Lock lock = mCacheLock.writeLock();

        try {
            mCache.clear();
            mCacheSize.set(0);
            trace("evictAll(): job events cache reset");

        } finally {
            lock.unlock();
        }
    }

    @Override
    public void evictEvent(String key) {
        if (key == null) return;

        final Lock lock = mCacheLock.writeLock();

        try {
            CacheEntry entry = mCache.get(key);
            evictEntry(entry);

        } finally {
            lock.unlock();
        }
    }

    @Override
    public JobEvent getEvent(String key) {
        if (key == null) return null;

        final Lock lock = mCacheLock.readLock();
        lock.lock();
        try {
            CacheEntry entry = mCache.get(key);

            if (entry == null) {
                return null;
            }

            long accessTime = SystemClock.elapsedRealtime();

            if (entry.timeoutMillis != -1 &&
                    (entry.accessTimeMillis + entry.timeoutMillis < accessTime)) {

                trace("getEvent(): cache entry expired for key %s", key);
                return null;
            }

            entry.accessTimeMillis = accessTime;

            trace("getEvent(): cache entry retrieved", entry);

            return entry.jobEvent;

        } finally {
            lock.unlock();
        }
    }

    @Override
    public void putEvent(JobEvent event, String key, CacheEntryDescriptor descriptor) {
        final Lock lock = mCacheLock.writeLock();
        lock.lock();
        try {
            long accessTime = SystemClock.elapsedRealtime();

            CacheEntry entry = new CacheEntry();
            entry.jobEvent = event;
            entry.timeoutMillis = descriptor.getExpirationTimeoutMillis();
            entry.accessTimeMillis = accessTime;

            final int sz = descriptor.getJobEventSize(event);
            if (sz < 0) {
                throw new IllegalArgumentException("job event size should be >= 0");
            }

            mCacheSize.addAndGet(sz);
            mCache.put(key, entry);

            trace("putEvent(): cache entry added; cache size: %s", entry, mCacheSize.get());

            trimCache();

        } finally {
            lock.unlock();
        }
    }

    private void evictEntry(CacheEntry entry) {
        if (entry == null || mCache.containsKey(entry.key) == false) return;

        mCache.remove(entry.key);
        int cacheSize = mCacheSize.getAndAdd(entry.size);

        trace("entry evicted; new cache size '%d'; last cache size: '%d'",
                entry, mCacheSize.get(), cacheSize);
    }

    private void trimCache() {
        int requiredSpace = mMaxCacheSize - mCacheSize.get();
        final long timeMillis = SystemClock.elapsedRealtime();
        final List<CacheEntry> entriesToEvict = new ArrayList<CacheEntry>();

        for (CacheEntry cacheEntry : mCache.values()) {
            if (cacheEntry.accessTimeMillis + cacheEntry.timeoutMillis > timeMillis) {
                trace("trimCache(): cache entry expired", cacheEntry.key);
                entriesToEvict.add(cacheEntry);

                if (requiredSpace > 0) {
                    int newRequiredSpace = requiredSpace - cacheEntry.size;
                    trace("trimCache(): decrease required space from '%d' to '%d' (-%d)",
                            cacheEntry, requiredSpace, newRequiredSpace, cacheEntry.size);
                    requiredSpace = newRequiredSpace;
                }

            } else if (requiredSpace > 0) {
                cacheEntry = getLeastEntry();
                trace("trimCache(): required space is '%s' (>0), retrieved least entry",
                        cacheEntry, requiredSpace);
                int newRequiredSpace = requiredSpace - cacheEntry.size;
                trace("trimCache(): decrease required space from '%d' to '%d' (-%d)",
                        cacheEntry, requiredSpace, newRequiredSpace, cacheEntry.size);
                requiredSpace = newRequiredSpace;
                entriesToEvict.add(cacheEntry);
            }
        }

        for (CacheEntry entry : entriesToEvict) {
            evictEntry(entry);
        }
    }

    private CacheEntry getLeastEntry() {
        long min = Long.MAX_VALUE;
        CacheEntry leastEntry = null;

        for (CacheEntry entry : mCache.values()) {
            if (entry.accessTimeMillis < min) {
                min = entry.accessTimeMillis;
                leastEntry = entry;
            }
        }

        return leastEntry;
    }

    private void trace(String msg, CacheEntry entry, Object... params) {
        if (mIsTraceEnabled == false) return;

        msg = String.format(msg, params);

        Log.d(LOG_TAG, String.format("%s; %s", msg, entry));
    }

    private void trace(String msg, Object... params) {
        if (mIsTraceEnabled == false) return;

        Log.d(LOG_TAG, String.format(msg, params));
    }
}
