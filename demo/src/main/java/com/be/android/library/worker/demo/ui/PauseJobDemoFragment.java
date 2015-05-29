package com.be.android.library.worker.demo.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
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
import com.be.android.library.worker.interfaces.Job;
import com.be.android.library.worker.models.JobParams;
import com.be.android.library.worker.models.ProgressUpdateEvent;
import com.be.android.library.worker.util.JobSelector;

import java.util.Map;

public class PauseJobDemoFragment extends BaseFragment
        implements CompoundButton.OnCheckedChangeListener {

    private static final String TAG_LOADER = "PauseJobDemoFragment_Loader";

    private ToggleButton mToggleButton;
    private ProgressBar mProgressBar;
    private TextView mProgressCountView;
    private TextView mStatusView;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // We can start job from any lifecycle point
        // and begin to listen to it's events later using JobEventDispatcher
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

        // Check if job is paused or is in progress
        final Job job = JobManager.getInstance().findJob(JobSelector.forJobTags(TAG_LOADER));
        if (job != null && !job.isFinished()) {
            final boolean isPaused = job.isPaused();
            mToggleButton.setChecked(isPaused);

            if (!isPaused) {
                mStatusView.setText("job is in progress");
            } else {
                mStatusView.setText("job paused");
            }
        }

        mToggleButton.setOnCheckedChangeListener(this);
    }

    @OnJobSuccess(PauseDemoJob.class)
    public void onPauseDemoJobFinished(JobEvent result) {
        mStatusView.setText("job finished");
        mProgressBar.setProgress(mProgressBar.getMax());
        mProgressCountView.setText(mProgressBar.getMax() + "%");
        mToggleButton.setEnabled(false);
    }

    @OnJobEvent(
            jobType = PauseDemoJob.class,
            eventCode = JobEvent.EVENT_CODE_UPDATE)
    public void onPauseDemoJobUpdateEvent(JobEvent event) {
        switch (event.getExtraCode()) {
            case JobEvent.EXTRA_CODE_PROGRESS_UPDATE:
                int progress = (int) (((ProgressUpdateEvent) event).getProgress() * mProgressBar.getMax());
                mProgressBar.setProgress(progress);
                mProgressCountView.setText(progress + "%");
                break;

            case JobEvent.EXTRA_CODE_FLAG_STATUS_CHANGED:
                Map.Entry<String, Boolean> flag = (Map.Entry<String, Boolean>) event.getPayload();
                if (JobParams.FLAG_JOB_PAUSED.equals(flag.getKey())) {
                    if (flag.getValue()) {
                        mStatusView.setText("job paused");
                    } else {
                        mStatusView.setText("job resumed");
                    }
                }
        }
    }

    @Override
    public boolean handleBackPress() {
        // Job will continue running after back press without this call
        // So you can reattach your UI to this job at any time before it finishes
        // JobManager.getInstance().cancelAll(JobSelector.forJobTags(TAG_LOADER));

        return super.handleBackPress();
    }
}
