package com.be.library.worker.annotations.compiler;

import com.be.library.worker.annotations.JobExtra;
import com.google.common.base.Strings;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;

public class JobExtraInfo extends FieldInfo {

    private static final String KEY_PREFIX = "EXTRA_";

    private final boolean mIsOptional;
    private final String mVariableKey;

    public JobExtraInfo(VariableElement variableElement,
                        ProcessingEnvironment env) throws IllegalArgumentException {

        super(variableElement, env);

        final JobExtra annotation = variableElement.getAnnotation(JobExtra.class);

        mIsOptional = annotation.optional();

        if (Strings.isNullOrEmpty(annotation.value())) {
            mVariableKey = JobProcessor.EXTRA_ANNOTATION_PRINTABLE +
                    "_" +
                    getSimpleJobName() +
                    "_" +
                    getVariableSimpleName();
        } else {
            mVariableKey = annotation.value();
        }
    }

    @Override
    public String getVariableKey() {
        return mVariableKey;
    }

    @Override
    public boolean isOptional() {
        return mIsOptional;
    }

    @Override
    public String getVariableKeyPrefix() {
        return KEY_PREFIX;
    }

    @Override
    public Class<?> getFieldAnnotationType() {
        return JobExtra.class;
    }
}