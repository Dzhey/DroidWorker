package com.be.android.library.worker.demo.ui;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.be.android.library.worker.annotations.OnJobFailure;
import com.be.android.library.worker.annotations.OnJobSuccess;
import com.be.android.library.worker.controllers.JobManager;
import com.be.android.library.worker.demo.R;
import com.be.android.library.worker.demo.jobs.SimpleImageLoaderJob;
import com.be.android.library.worker.demo.jobs.SimpleImageLoaderJobExtras;
import com.be.android.library.worker.demo.ui.base.BaseFragment;
import com.be.android.library.worker.demo.ui.base.TitleProvider;
import com.be.android.library.worker.interfaces.Job;
import com.be.android.library.worker.models.LoadJobResult;
import com.be.android.library.worker.util.JobSelector;

public class SimpleLoadDemoFragment extends BaseFragment implements TitleProvider {

    private static final String TAG_IMAGE_LOADER = "SimpleLoadDemoFragment_loader";
    private static final String IMG_URL =
            "http://images.techhive.com/images/article/2016/11/android-nougat-eating-100692101-large.jpg";

    private ProgressBar mProgressBar;
    private ImageView mImageView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_network_load_demo, container, false);

        mProgressBar = (ProgressBar) view.findViewById(android.R.id.progress);
        mImageView = (ImageView) view.findViewById(R.id.image);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requestLoad(TAG_IMAGE_LOADER);
    }

    @Override
    public Job onCreateJob(String attachTag) {
        return new SimpleImageLoaderJob()
                .setup()
                // Use JobManager.JOB_GROUP_UNIQUE to execute job asynchronously
                // and prevent other jobs observe having to wait for completion of this job
                .group(JobManager.JOB_GROUP_UNIQUE)
                .configure(SimpleImageLoaderJobExtras.captureExtras()
                        .setImageUrl(IMG_URL)
                        .apply())
                .getJob();
    }

    @OnJobSuccess(SimpleImageLoaderJob.class)
    public void onImageLoaded(LoadJobResult<Bitmap> result) {
        mImageView.setImageBitmap(result.getData());
        mImageView.setBackgroundColor(Color.TRANSPARENT);
        animateImageView();
        mProgressBar.setVisibility(View.GONE);
    }

    @OnJobFailure(SimpleImageLoaderJob.class)
    public void onImageLoadFailed() {
        showToast("Load failed");
        mImageView.setBackgroundColor(getResources().getColor(R.color.translucentRed));
        mProgressBar.setVisibility(View.GONE);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void animateImageView() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return;
        }

        ValueAnimator animator = ValueAnimator.ofInt(0, 255);
        animator.setDuration(300);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mImageView.setImageAlpha((Integer) valueAnimator.getAnimatedValue());
            }
        });
        animator.start();
    }

    @Override
    public boolean handleBackPress() {
        // Job will continue running when back is pressed without this call
        JobManager.getInstance().cancelAll(JobSelector.forJobTags(TAG_IMAGE_LOADER));

        return super.handleBackPress();
    }

    @Override
    public String getTitle(Resources resources) {
        return resources.getString(R.string.simple_loader_demo);
    }
}
