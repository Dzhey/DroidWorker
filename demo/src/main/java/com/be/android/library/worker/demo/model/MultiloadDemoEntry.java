package com.be.android.library.worker.demo.model;

public class MultiloadDemoEntry {

    private final int mItemId;
    private final String mLoadResult;

    public MultiloadDemoEntry(int itemId, String loadResult) {
        mItemId = itemId;
        mLoadResult = loadResult;
    }

    public int getItemId() {
        return mItemId;
    }

    public String getLoadResult() {
        return mLoadResult;
    }
}
