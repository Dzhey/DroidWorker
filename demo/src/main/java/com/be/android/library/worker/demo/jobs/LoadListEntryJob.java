package com.be.android.library.worker.demo.jobs;

import android.util.Log;

import com.be.android.library.worker.demo.model.MultiloadDemoEntry;
import com.be.android.library.worker.jobs.LoadJob;
import com.be.android.library.worker.models.LoadJobResult;

public class LoadListEntryJob extends LoadJob {

    private static final String LOG_TAG = LoadListEntryJob.class.getSimpleName();
    private final int mListEntryId;

    public LoadListEntryJob(int listEntryId) {
        mListEntryId = listEntryId;
    }

    @Override
    protected void onPreExecute() throws Exception {
        super.onPreExecute();

        Log.i(LOG_TAG, "Loading data for list entry id:" + String.valueOf(mListEntryId));
    }

    @Override
    protected LoadJobResult<MultiloadDemoEntry> performLoad() throws Exception {
        Thread.sleep(500);

        final MultiloadDemoEntry resultData = new MultiloadDemoEntry(mListEntryId, "Success!");

        return new LoadJobResult<MultiloadDemoEntry>(resultData);
    }

}
