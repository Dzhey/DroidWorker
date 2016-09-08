package com.be.library.worker.rxbindings;

import com.be.android.library.worker.base.JobEvent;
import com.be.android.library.worker.base.JobStatus;
import com.be.android.library.worker.controllers.JobManager;
import com.be.android.library.worker.exceptions.JobExecutionException;
import com.be.android.library.worker.interfaces.Job;
import com.be.android.library.worker.interfaces.JobEventListener;
import com.be.android.library.worker.util.JobSelector;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;

/**
 * @author Eugene Byzov gdzhey@gmail.com
 *         Created on 08-Sep-16.
 */
public final class RxJobs {

    private static final ValueHolder<RxJobs> INSTANCE;

    static {
        INSTANCE = ValueHolder.of(new Provider<RxJobs>() {
            @Override
            public RxJobs get() {
                return new Builder().build();
            }
        });
    }

    /**
     * @return default RxJobs instance
     */
    public static RxJobs get() {
        return INSTANCE.getValue();
    }

    private final Settings mSettings;

    protected RxJobs(Settings settings) {
        mSettings = settings;
    }

    /**
     * <p>
     * Create <code>Observable</code> for specified <code>selector</code>.
     * Subscriber will receive events sent from jobs
     * described by <code>selector</code>.
     * <code>JobEvent</code>s are arranged accordingly to
     * <code>RxJobs</code> presets set by {@link Builder}.
     * </p>
     * <p>
     * Once all jobs are finished {@link Subscriber#onCompleted()} is invoked.
     * </p>
     *
     * @param jobManager job manager to apply selector on
     * @param selector   selector describing jobs
     * @return new Observable
     * @see JobManager
     * @see JobSelector
     * @see RxJobs#observe(JobSelector)
     */
    public Observable<JobEvent> observe(final JobManager jobManager,
                                        final JobSelector selector) {

        final List<Job> pendingJobs = new ArrayList<>();
        final List<Integer> pendingJobIds = new ArrayList<>();

        for (Job job : jobManager.findAll(selector)) {
            if (!job.isFinishedOrCancelled()) {
                pendingJobs.add(job);
                pendingJobIds.add(job.getJobId());
            }
        }

        if (pendingJobs.isEmpty()) {
            return Observable.empty();
        }

        return Observable.create(new Observable.OnSubscribe<JobEvent>() {
            @Override
            public void call(final Subscriber<? super JobEvent> subscriber) {
                final CompositeEventListener jobsEventListener =
                        new CompositeEventListener(pendingJobIds);

                jobsEventListener.setDelegate(new JobEventListener() {
                    @Override
                    public void onJobEvent(JobEvent event) {
                        if (mSettings.treatFailureAsError
                                && event.getJobStatus() == JobStatus.FAILED) {

                            subscriber.onError(new JobExecutionException(event));
                            return;
                        }

                        if (mSettings.treatCancellationAsError
                                && event.getJobStatus() == JobStatus.CANCELLED) {

                            subscriber.onError(new JobExecutionException(event));
                            return;
                        }

                        if (mSettings.provideIntermediateEvents || event.isJobFinished()) {
                            subscriber.onNext(event);
                        }

                        if (jobsEventListener.isCompleted()) {
                            subscriber.onCompleted();
                        }
                    }
                });

                jobManager.addJobEventListener(jobsEventListener);
            }
        });
    }

    public Observable<JobEvent> observe(JobSelector selector) {
        return observe(JobManager.getInstance(), selector);
    }

    /**
     * Allows to create customized {@link RxJobs} instance
     */
    public static class Builder {

        private final Settings mSettings;

        public Builder() {
            mSettings = new Settings();
        }

        /**
         * <p>
         * Direct any {@link JobEvent} with {@link JobStatus#FAILED}
         * to {@link Subscriber#onError(Throwable)}. In such case <code>Throwable</code> is
         * represented by {@link JobExecutionException} with defined <code>JobEvent</code>.
         * </p>
         * <p><code>true</code> by default</p>
         *
         * @param treatFailureAsError true to direct failures to <code>onError</code>
         * @return this
         */
        public Builder treatFailureAsError(boolean treatFailureAsError) {
            mSettings.treatFailureAsError = treatFailureAsError;

            return this;
        }

        /**
         * <p>
         * Direct any {@link JobEvent} with {@link JobStatus#CANCELLED}
         * to {@link Subscriber#onError(Throwable)}. In such case <code>Throwable</code> is
         * represented by {@link JobExecutionException} with defined <code>JobEvent</code>.
         * </p>
         * <p><code>true</code> by default</p>
         *
         * @param treatCancellationAsError true to direct cancellation events
         *                                 to <code>onError</code>
         * @return this
         */
        public Builder treatCancellationAsError(boolean treatCancellationAsError) {
            mSettings.treatCancellationAsError = treatCancellationAsError;

            return this;
        }

        /**
         * <p>
         * Determine whether {@link Subscriber} should receive all events.
         * <br><code>Subscriber</code> will receive all job events including status updates
         * and custom events sent through {@link Job#notifyJobEvent(JobEvent)} if set
         * to <code>true</code>. Set to <code>false</code> to receive only last (job finish) event.
         * </p>
         * <p><code>true</code> by default</p>
         *
         * @param provideIntermediateEvents set to true to receive all events
         * @return this
         */
        public Builder provideIntermediateEvents(boolean provideIntermediateEvents) {
            mSettings.provideIntermediateEvents = provideIntermediateEvents;

            return this;
        }

        public RxJobs build() {
            final Settings settings = new Settings();
            settings.treatFailureAsError = mSettings.treatFailureAsError;
            settings.treatCancellationAsError = mSettings.treatCancellationAsError;
            settings.provideIntermediateEvents = mSettings.provideIntermediateEvents;

            return new RxJobs(settings);
        }
    }

    private static class Settings {
        private boolean treatFailureAsError = true;
        private boolean treatCancellationAsError = true;
        private boolean provideIntermediateEvents = true;
    }
}
