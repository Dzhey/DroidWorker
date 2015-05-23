package com.be.android.library.worker.controllers;

import android.text.TextUtils;

import com.be.android.library.worker.handlers.JobEventHandlerInterface;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

public class JobLoaderManager {

    public static final int LOADER_RETENTION_THRESHOLD_DEFAULT = 5;

    private static final JobLoaderManager INSTANCE = new JobLoaderManager();

    private final Map<String, SoftReference<JobLoader>> mLoaders;
    private final Map<String, JobLoader> mHeldLoaders;

    public static JobLoaderManager getInstance() {
        return INSTANCE;
    }

    private int mLoaderRetentionThreshold = LOADER_RETENTION_THRESHOLD_DEFAULT;

    public JobLoaderManager() {
        mLoaders = new HashMap<String, SoftReference<JobLoader>>();
        mHeldLoaders = new HashMap<String, JobLoader>(LOADER_RETENTION_THRESHOLD_DEFAULT);
    }

    public int getLoaderRetentionThreshold() {
        return mLoaderRetentionThreshold;
    }

    public void setLoaderRetentionThreshold(int loaderRetentionThreshold) {
        mLoaderRetentionThreshold = loaderRetentionThreshold;
    }

    public JobLoader initLoader(JobEventHandlerInterface eventHandler,
                                String loaderAttachTag,
                                JobLoader.JobLoaderCallbacks callbacks) {

        if (TextUtils.isEmpty(loaderAttachTag)) {
            throw new IllegalAccessError("loaderAttachTag may not be empty");
        }

        JobLoader loader = findLoader(loaderAttachTag);
        if (loader != null) {
            loader.setCallbacks(callbacks);
            loader.setEventHandler(eventHandler);
        } else {
            loader = createNewLoader(eventHandler, loaderAttachTag, callbacks);
        }

        holdLoader(loaderAttachTag, loader);

        return loader;
    }

    protected JobLoader createNewLoader(JobEventHandlerInterface eventHandler,
                                      String loaderAttachTag, JobLoader.JobLoaderCallbacks callbacks) {

        JobLoader loader = new JobLoader(getJobManager(), eventHandler, loaderAttachTag, callbacks);
        mLoaders.put(loaderAttachTag, new SoftReference<JobLoader>(loader));

        return loader;
    }

    protected JobLoader findLoader(String loaderTag) {
        if (mHeldLoaders.containsKey(loaderTag)) {
            return mHeldLoaders.get(loaderTag);
        }

        if (mLoaders.containsKey(loaderTag)) {
            SoftReference<JobLoader> loaderRef = mLoaders.get(loaderTag);

            return loaderRef.get();
        }

        return null;
    }

    protected JobLoader holdLoader(String loaderTag, JobLoader loader) {
        if (loader == null) return null;

        if (mHeldLoaders.size() >= mLoaderRetentionThreshold) {
            mHeldLoaders.remove(loaderTag);
        }

        mHeldLoaders.put(loaderTag, loader);

        return loader;
    }

    protected JobManager getJobManager() {
        return JobManager.getInstance();
    }
}
