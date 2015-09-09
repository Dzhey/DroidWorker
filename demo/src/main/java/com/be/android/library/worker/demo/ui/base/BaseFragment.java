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

import java.util.List;

public class BaseFragment extends Fragment implements JobLoader.JobLoaderCallbacks  {

    private JobEventDispatcher mEventDispatcher;

    private final JobLoader.JobLoaderCallbacks mJobLoaderCallbacks =
            new JobLoader.JobLoaderCallbacks() {
        @Override
        public Job onCreateJob(String attachTag, Bundle data) {
            final Job job = BaseFragment.this.onCreateJob(attachTag, data);

            if (job == null) {
                return BaseFragment.this.onCreateJob(attachTag);
            }

            return job;
        }
    };

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

    protected int requestLoad(String loaderAttachTag, JobLoader.JobLoaderCallbacks callbacks, Bundle data) {
        JobLoaderManager mgr = JobLoaderManager.getInstance();
        JobLoader loader = mgr.initLoader(mEventDispatcher, loaderAttachTag, callbacks);

        return loader.requestLoad(data);
    }

    protected int requestLoad(String loaderAttachTag, Bundle data) {
        return requestLoad(loaderAttachTag, mJobLoaderCallbacks, data);
    }

    protected int requestLoad(String loaderAttachTag) {
        return requestLoad(loaderAttachTag, mJobLoaderCallbacks, null);
    }

    protected int requestReload(String loaderAttachTag,
                                JobLoader.JobLoaderCallbacks callbacks,
                                Bundle data,
                                boolean discardCallbacks) {

        final JobSelector selector = JobSelector.forJobTags(loaderAttachTag);
        if (discardCallbacks) {
            List<Job> jobs = JobManager.getInstance().findAll(selector);
            for (Job job : jobs) {
                mEventDispatcher.removePendingJob(job.getJobId());
                job.cancel();
            }
        } else {
            JobManager.getInstance().cancelAll(selector);
        }

        JobLoaderManager mgr = JobLoaderManager.getInstance();
        JobLoader loader = mgr.initLoader(mEventDispatcher, loaderAttachTag, callbacks);

        final int jobId = loader.requestLoad(data);

        onReloadRequested(loaderAttachTag, jobId);

        return jobId;
    }

    protected int requestReload(String loaderAttachTag, Bundle data) {
        return requestReload(loaderAttachTag, mJobLoaderCallbacks, data, false);
    }

    protected int requestReload(String loaderAttachTag) {
        return requestReload(loaderAttachTag, mJobLoaderCallbacks, null, false);
    }

    protected int requestReload(String loaderAttachTag, Bundle data, boolean discardCallbacks) {
        return requestReload(loaderAttachTag, mJobLoaderCallbacks, data, discardCallbacks);
    }

    protected int requestReload(String loaderAttachTag, boolean discardCallbacks) {
        return requestReload(loaderAttachTag, mJobLoaderCallbacks, null, discardCallbacks);
    }

    protected void onReloadRequested(String loaderAttachTag, int jobId) {
    }

    @Override
    public Job onCreateJob(String attachTag, Bundle data) {
        return null;
    }

    protected Job onCreateJob(String attachTag) {
        throw new UnsupportedOperationException("should implement onCreateJob");
    }
}