package com.be.android.library.worker.demo.ui;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ViewSwitcher;

import com.be.android.library.worker.annotations.OnJobCancelled;
import com.be.android.library.worker.annotations.OnJobFailure;
import com.be.android.library.worker.annotations.OnJobSuccess;
import com.be.android.library.worker.controllers.JobManager;
import com.be.android.library.worker.demo.R;
import com.be.android.library.worker.demo.jobs.CancelableJob;
import com.be.android.library.worker.demo.ui.base.BaseFragment;
import com.be.android.library.worker.demo.ui.base.TitleProvider;
import com.be.android.library.worker.interfaces.Job;
import com.be.android.library.worker.util.JobSelector;

public class JobCancelDemoFragment extends BaseFragment implements TitleProvider {

    private static final String TAG_LOADER = "JobCancelDemoFragment_loader";

    private ViewSwitcher mViewSwitcher;
    private Button mCancelButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_job_cancel_demo, container, false);

        mViewSwitcher = (ViewSwitcher) view.findViewById(R.id.switcher);
        mCancelButton = (Button) view.findViewById(R.id.buttonCancel);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requestLoad(TAG_LOADER);
    }

    @Override
    public Job onCreateJob(String attachTag) {
        return new CancelableJob();
    }

    @OnJobSuccess(CancelableJob.class)
    public void onJobFinished() {
        mViewSwitcher.showNext();
    }

    @OnJobFailure(CancelableJob.class)
    public void onJobFailed() {
        mViewSwitcher.showNext();
    }

    @OnJobCancelled(CancelableJob.class)
    public void onJobCancelled() {
        mViewSwitcher.showNext();
    }

    @Override
    public void onResume() {
        super.onResume();

        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCancelButton.setEnabled(false);
                JobManager.getInstance().cancelAll(JobSelector.forJobTags(TAG_LOADER));
            }
        });
    }

    @Override
    public boolean handleBackPress() {
        // Job will continue running when back is pressed without this call
        JobManager.getInstance().cancelAll(JobSelector.forJobTags(TAG_LOADER));

        return super.handleBackPress();
    }

    @Override
    public String getTitle(Resources resources) {
        return resources.getString(R.string.cancel_demo);
    }
}
