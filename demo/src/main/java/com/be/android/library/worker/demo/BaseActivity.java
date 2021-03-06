package com.be.android.library.worker.demo;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.be.android.library.worker.controllers.JobLoader;
import com.be.android.library.worker.controllers.JobLoaderManager;
import com.be.android.library.worker.handlers.JobEventDispatcher;
import com.be.android.library.worker.interfaces.Job;
import com.be.android.library.worker.util.JobSelector;

/**
 * Base activity implementation defines common interface
 * to submit jobs and automatically register to job events
 */
public class BaseActivity extends ActionBarActivity implements JobLoader.JobLoaderCallbacks {

    private JobEventDispatcher mJobEventDispatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mJobEventDispatcher = new JobEventDispatcher(this);
        mJobEventDispatcher.restoreState(savedInstanceState);
    }

    protected void registerForJobEvents() {
        // Implementations should have at least one job listener to successfully register
        mJobEventDispatcher.register(this);
    }

    protected void unregisterForJobEvents() {
        mJobEventDispatcher.unregister(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        mJobEventDispatcher.saveState(outState);
    }

    protected int submitJob(Job job) {
        return mJobEventDispatcher.submitJob(job);
    }

    protected int requestLoad(String loaderAttachTag) {
        JobLoaderManager mgr = JobLoaderManager.getInstance();
        JobLoader loader = mgr.initLoader(mJobEventDispatcher, loaderAttachTag, this);

        return loader.requestLoad(null);
    }

    protected int requestReload(String loaderAttachTag) {
        App.getJobManager().cancelAll(JobSelector.forJobTags(loaderAttachTag));

        return requestLoad(loaderAttachTag);
    }

    @Override
    public Job onCreateJob(String attachTag, Bundle data) {
        throw new UnsupportedOperationException("should implement onCreateJob");
    }

    public JobEventDispatcher getJobEventDispatcher() {
        return mJobEventDispatcher;
    }

    public void setJobEventDispatcher(JobEventDispatcher dispatcher) {
        mJobEventDispatcher = dispatcher;
    }
}
