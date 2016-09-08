package com.be.android.library.worker.demo.ui;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.controllers.JobManager;
import com.be.android.library.worker.demo.R;
import com.be.android.library.worker.demo.jobs.SimpleImageLoaderJob;
import com.be.android.library.worker.demo.jobs.SimpleImageLoaderJobExtras;
import com.be.android.library.worker.demo.ui.base.BaseFragment;
import com.be.android.library.worker.demo.ui.base.TitleProvider;
import com.be.android.library.worker.exceptions.JobExecutionException;
import com.be.android.library.worker.interfaces.Job;
import com.be.android.library.worker.models.LoadJobResult;
import com.be.android.library.worker.util.JobSelector;
import com.be.library.worker.rxbindings.RxJobs;

import rx.android.schedulers.AndroidSchedulers;

public class SimpleRxLoadDemoFragment extends BaseFragment implements TitleProvider {

    private static final String TAG_IMAGE_LOADER = "SimpleRxLoadDemoFragment_loader";
    private static final String IMG_URL =
            "https://lh3.googleusercontent.com/-Qp7luSMArJs/AAAAAAAAAAI/AAAAAAAAAAU/B7YxYtLi_co/s265-c-k-no/photo.jpg";

    private ProgressBar mProgressBar;
    private ImageView mImageView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_network_load_demo, container, false);

        mProgressBar = (ProgressBar) view.findViewById(android.R.id.progress);
        mImageView = (ImageView) view.findViewById(R.id.image);

        ((TextView)view.findViewById(R.id.description))
                .setText(R.string.simple_rx_loader_demo_description);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mProgressBar.getVisibility() == View.VISIBLE) {
            // take last event and extract job result
            registerSubscription(RxJobs.get().observe(
                    JobSelector.forJobId(requestLoad(TAG_IMAGE_LOADER)))
                        .observeOn(AndroidSchedulers.mainThread())
                        .last()
                        .cast(LoadJobResult.class)
                        .map(result -> (Bitmap) result.getData())
                        .subscribe(this::onImageLoaded, this::onImageLoadFailed));
        }
    }

    @Override
    public Job onCreateJob(String attachTag) {
        return new SimpleImageLoaderJob()
                .setup()
                .group(JobManager.JOB_GROUP_UNIQUE)
                .configure(SimpleImageLoaderJobExtras.captureExtras()
                        .setImageUrl(IMG_URL)
                        .apply())
                .getJob();
    }

    void onImageLoaded(Bitmap image) {
        mImageView.setImageBitmap(image);
        mImageView.setBackgroundColor(Color.TRANSPARENT);
        animateImageView();
        mProgressBar.setVisibility(View.GONE);
    }

    void onImageLoadFailed(Throwable error) {
        final JobEvent errorEvent = ((JobExecutionException) error).getJobEvent();
        showToast("Load: " + errorEvent.getJobStatus());

        mImageView.setBackgroundColor(ContextCompat.getColor(
                getActivity(), R.color.translucentRed));
        mProgressBar.setVisibility(View.GONE);
    }

    @Override
    protected void registerEventDispatcher() {
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void animateImageView() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return;
        }

        ValueAnimator animator = ValueAnimator.ofInt(0, 255);
        animator.setDuration(300);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(anim -> mImageView.setImageAlpha(
                (Integer) anim.getAnimatedValue()));
        animator.start();
    }

    @Override
    public String getTitle(Resources resources) {
        return resources.getString(R.string.simple_loader_demo);
    }
}
