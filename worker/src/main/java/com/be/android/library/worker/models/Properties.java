package com.be.android.library.worker.models;

import com.be.android.library.worker.base.BaseJob;

/**
 * Properties used by JobManager
 *
 * Created by Eugene B. on 09/04/16.
 */
public class Properties {

    private boolean mIsAutoInjectUsed;
    private boolean mIsDebugOutputEnabled;

    /**
     * @return true if each {@link com.be.android.library.worker.base.BaseJob}
     * should inject defined extras in {@link BaseJob#onPreExecute()}
     * @see BaseJob#onPreExecute()
     */
    public boolean isAutoInjectUsed() {
        return mIsAutoInjectUsed;
    }

    public void setIsAutoInjectUsed(boolean isAutoInjectUsed) {
        mIsAutoInjectUsed = isAutoInjectUsed;
    }

    public boolean isDebugOutputEnabled() {
        return mIsDebugOutputEnabled;
    }

    public void setIsDebugOutputEnabled(boolean isDebugOutputEnabled) {
        mIsDebugOutputEnabled = isDebugOutputEnabled;
    }
}
