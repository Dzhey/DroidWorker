package com.be.android.library.worker.util;

import com.be.android.library.worker.exceptions.JobCreationException;
import com.be.android.library.worker.interfaces.Job;
import com.be.android.library.worker.interfaces.JobCreator;
import com.be.android.library.worker.interfaces.JobFactory;
import com.be.android.library.worker.models.JobParams;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

public class ReflectiveJobFactory implements JobFactory {

    public static final String CREATOR_FIELD_NAME = "JOB_CREATOR";
    private final JobParams mParams;

    public ReflectiveJobFactory(JobParams params) {
        mParams = params;
    }

    public JobParams getParams() {
        return mParams;
    }

    @Override
    public Job createJob() {
        final Class<?> clazz = getJobClass(mParams);

        if (!mParams.checkFlag(JobParams.FLAG_USE_JOB_CREATOR)) {
            return constructJob(clazz, mParams);
        }

        final JobCreator<?> creator = getJobCreator(clazz, mParams);

        return constructJob(clazz, mParams, creator);
    }

    protected Job constructJob(Class<?> jobClazz, JobParams params, JobCreator<?> creator) {
        final Job job = creator.createJob(params);

        return job.setup().params(params).getJob();
    }

    protected JobCreator<?> getJobCreator(Class<?> jobClazz, JobParams params) {
        try {
            final Field creatorField = jobClazz.getDeclaredField(CREATOR_FIELD_NAME);

            return (JobCreator<?>) creatorField.get(jobClazz);

        } catch (Exception e) {
            throw new JobCreationException(String.format(
                    "Failed to find creator for job '%s'; " +
                            "Please ensure field is public, static and have appropriate type",
                    jobClazz.getName()));
        }
    }

    protected Job constructJob(Class<?> clazz, JobParams params) throws JobCreationException {
        try {
            final Constructor<?> constructor = clazz.getDeclaredConstructor();

            final Job job = (Job) constructor.newInstance();

            job.setup().params(params);

            return job;

        } catch (ClassCastException e) {
            throw new JobCreationException(String.format("Class '%s' is not Job",
                    clazz.getName()));

        } catch (Exception e) {
            throw new JobCreationException(String.format("failed to find job class '%s' constructor; " +
                    "Job should have default public constructor or JOB_CREATOR field",
                    clazz.getName()));
        }
    }

    protected Class<?> getJobClass(JobParams params) throws JobCreationException {
        if (!params.hasExtra(JobParams.EXTRA_JOB_TYPE)) {
            throw new JobCreationException("unable to instantiate job; no class defined for job; " +
                            "please use JobParams.EXTRA_JOB_TYPE to define job class");
        }

        final String type = (String) params.getExtra(JobParams.EXTRA_JOB_TYPE);
        if (type == null || type.length() == 0) {
            throw new JobCreationException(
                    String.format("failed to create job; invalid job class defined: '%s'", type));
        }

        try {
            return Class.forName(type);

        } catch (ClassNotFoundException e) {
            throw new JobCreationException(String.format("failed to find job class '%s'", type), e);
        }
    }
}
