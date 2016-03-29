package com.be.library.worker.annotations.compiler;

import com.be.library.worker.annotations.JobFlag;
import com.google.common.base.Strings;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;

public class JobFlagInfo extends FieldInfo {

    private static final String KEY_PREFIX = "FLAG_";

    private final boolean mIsOptional;
    private final String mVariableKey;

    public JobFlagInfo(VariableElement variableElement,
                        ProcessingEnvironment env) throws IllegalArgumentException {

        super(variableElement, env);

        final ErrorReporter errorReporter = new ErrorReporter(env);

        if (!variableElement.asType().getKind().equals(TypeKind.BOOLEAN)) {
            errorReporter.abortWithError(String.format("flag '%s' value can only be 'boolean'",
                    variableElement.getSimpleName().toString()));
        }

        final JobFlag annotation = variableElement.getAnnotation(JobFlag.class);

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
        return JobFlag.class;
    }
}
