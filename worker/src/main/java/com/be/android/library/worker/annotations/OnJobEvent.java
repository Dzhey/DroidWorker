package com.be.android.library.worker.annotations;

import com.be.android.library.worker.base.JobStatus;
import com.be.android.library.worker.interfaces.Job;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface OnJobEvent {
    public Class<? extends Job> value() default Job.class;
    public Class<? extends Job> jobType() default Job.class;
    public String[] jobTags() default {};
    public JobStatus[] jobStatus() default {};
    public int[] eventCode() default {};
}
