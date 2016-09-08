package com.be.android.library.worker.demo.ui;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.be.android.library.worker.annotations.OnJobEvent;
import com.be.android.library.worker.annotations.OnJobSuccess;
import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.controllers.JobManager;
import com.be.android.library.worker.demo.R;
import com.be.android.library.worker.demo.jobs.PauseDemoJob;
import com.be.android.library.worker.demo.ui.base.BaseFragment;
import com.be.android.library.worker.demo.ui.base.TitleProvider;
import com.be.android.library.worker.interfaces.Job;
import com.be.android.library.worker.models.FlagChangeEvent;
import com.be.android.library.worker.models.JobParams;
import com.be.android.library.worker.models.ProgressUpdateEvent;
import com.be.android.library.worker.util.JobSelector;

import java.util.Map;

public class PauseJobDemoFragment extends BaseFragment
        implements CompoundButton.OnCheckedChangeListener, TitleProvider {

    private static final String TAG_LOADER = "PauseJobDemoFragment_Loader";

    private ToggleButton mToggleButton;
    private ProgressBar mProgressBar;
    private TextView mProgressCountView;
    private TextView mStatusView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.menu_pause_job_demo, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_restart:
                requestReload(TAG_LOADER, true);
                return true;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onReloadRequested(String loaderAttachTag, int jobId) {
        mStatusView.setText("job is in progress");
        mProgressBar.setProgress(0);
        mProgressCountView.setText("0%");
        mToggleButton.setChecked(false);
        mToggleButton.setEnabled(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pause_job_demo, container, false);

        mToggleButton = (ToggleButton) view.findViewById(R.id.toggleButton);

        mProgressBar = (ProgressBar) view.findViewById(android.R.id.progress);
        mProgressCountView = (TextView) view.findViewById(R.id.progressCountView);
        mStatusView = (TextView) view.findViewById(R.id.statusView);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // We can start job observe any activity's or fragment's lifecycle point
        // and begin to listen to it's events using JobEventDispatcher
        // BaseFragment subscribes to job events observe onResume()
        requestLoad(TAG_LOADER);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        final Job job = JobManager.getInstance().findJob(JobSelector.forJobTags(TAG_LOADER));
        if (job != null) {
            if (isChecked) {
                job.pause();
            } else {
                job.unpause();
            }
        } else {
            showToast("job is not running");
        }
    }

    @Override
    public Job onCreateJob(String attachTag) {
        PauseDemoJob job = new PauseDemoJob();

        job.setup().group(JobManager.JOB_GROUP_UNIQUE);

        return job;
    }

    @Override
    public void onResume() {
        super.onResume();

        final PauseDemoJob job = (PauseDemoJob) JobManager.getInstance().findJob(
                JobSelector.forJobTags(TAG_LOADER));

        if (job != null) {
            syncUiToJob(job);
        }

        mToggleButton.setOnCheckedChangeListener(this);
    }

    private void syncUiToJob(PauseDemoJob job) {
        updateProgressBar(job.getProgress());

        if (!job.isFinished()) {
            final boolean isPaused = job.isPaused();
            mToggleButton.setChecked(isPaused);

            if (!isPaused) {
                mStatusView.setText("job is in progress");
            } else {
                mStatusView.setText("job paused");
            }
        }
    }

    private void updateProgressBar(float progress) {
        int progressInt = (int) (progress * mProgressBar.getMax());
        mProgressBar.setProgress(progressInt);
        mProgressCountView.setText(progressInt + "%");
    }

    @OnJobSuccess(PauseDemoJob.class)
    public void onPauseDemoJobFinished(JobEvent result) {
        mStatusView.setText("job finished");
        mProgressBar.setProgress(mProgressBar.getMax());
        mProgressCountView.setText(mProgressBar.getMax() + "%");
        mToggleButton.setEnabled(false);
    }

    @OnJobEvent(PauseDemoJob.class)
    public void onPauseDemoJobProgressUpdate(ProgressUpdateEvent event) {
        updateProgressBar(event.getProgress());
    }

    @OnJobEvent(PauseDemoJob.class)
    public void onPauseDemoJobUpdateEvent(FlagChangeEvent event) {
        if (JobParams.FLAG_JOB_PAUSED.equals(event.getFlag().getName())) {
            if (event.getFlag().getValue()) {
                mStatusView.setText("job paused");
            } else {
                mStatusView.setText("job resumed");
            }
        }
    }

    @Override
    public boolean handleBackPress() {
        // Job will continue running when back is pressed without this call
        // So you can reattach your UI to this job at any time before it finishes
        // JobManager.getInstance().cancelAll(JobSelector.forJobTags(TAG_LOADER));

        return super.handleBackPress();
    }

    @Override
    public String getTitle(Resources resources) {
        return resources.getString(R.string.pause_demo);
    }
}
