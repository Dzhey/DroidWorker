package com.be.android.library.worker.demo.ui.base;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import com.be.android.library.worker.controllers.JobLoader;
import com.be.android.library.worker.controllers.JobLoaderManager;
import com.be.android.library.worker.controllers.JobManager;
import com.be.android.library.worker.handlers.JobEventDispatcher;
import com.be.android.library.worker.interfaces.Job;
import com.be.android.library.worker.util.JobSelector;

public class BaseFragment extends Fragment implements JobLoader.JobLoaderCallbacks  {

    private JobEventDispatcher mEventDispatcher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mEventDispatcher = new JobEventDispatcher(getActivity());
        mEventDispatcher.restoreState(savedInstanceState);
    }

    @Override
    public void onPause() {
        mEventDispatcher.unregister(this);

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        mEventDispatcher.register(this);
    }

    public boolean handleBackPress() {
        return false;
    }

    protected JobEventDispatcher getJobEventDispatcher() {
        return mEventDispatcher;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        mEventDispatcher.saveState(outState);
    }

    protected void showToast(int stringId) {
        if (!isVisible()) return;

        showToast(stringId, Toast.LENGTH_SHORT);
    }

    protected void showToast(String text) {
        if (!isVisible()) return;

        showToast(text, Toast.LENGTH_SHORT);
    }

    protected void showToast(int stringId, int toastLength) {
        if (!isVisible()) return;

        Toast.makeText(getActivity(), stringId, toastLength).show();
    }

    protected void showToast(String text, int toastLength) {
        if (!isVisible()) return;

        Toast.makeText(getActivity(), text, toastLength).show();
    }

    protected int submitJob(Job job) {
        return mEventDispatcher.submitJob(job);
    }

    protected int requestLoad(String loaderAttachTag, JobLoader.JobLoaderCallbacks callbacks) {
        JobLoaderManager mgr = JobLoaderManager.getInstance();
        JobLoader loader = mgr.initLoader(mEventDispatcher, loaderAttachTag, callbacks);

        return loader.requestLoad();
    }
    protected int requestLoad(String loaderAttachTag) {
        return requestLoad(loaderAttachTag, this);
    }

    protected int requestReload(String loaderAttachTag, JobLoader.JobLoaderCallbacks callbacks) {
        JobManager.getInstance().cancelAll(JobSelector.forJobTags(loaderAttachTag));

        JobLoaderManager mgr = JobLoaderManager.getInstance();
        JobLoader loader = mgr.initLoader(mEventDispatcher, loaderAttachTag, callbacks);

        return loader.requestLoad();
    }

    protected int requestReload(String loaderAttachTag) {
        return requestReload(loaderAttachTag, this);
    }

    @Override
    public Job onCreateJob(String attachTag) {
        throw new UnsupportedOperationException("should implement onCreateJob");
    }
}
