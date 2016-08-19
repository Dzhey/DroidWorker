package com.be.library.worker.annotations.compiler;

import com.be.library.worker.annotations.JobExtra;
import com.be.library.worker.annotations.Shared;
import com.google.common.collect.Lists;

import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.util.ElementFilter;

public class SharedFieldInfo extends FieldInfo {

    private final ProcessingEnvironment mProcEnv;
    private final ErrorReporter mErrorReporter;
    private final VariableElement mElement;
    private String mVariableKey;
    private String mKeyPrefix;
    private boolean mIsOptional;
    private Class<?> mFieldAnnotationType;

    public SharedFieldInfo(VariableElement variableElement,
                           ProcessingEnvironment env) throws IllegalArgumentException {

        super(variableElement, env);

        mProcEnv = env;
        mErrorReporter = new ErrorReporter(env);
        mElement = variableElement;
    }

    @Override
    public void init() {
        super.init();

        final TypeElement parentJobType = getOriginShareTarget();
        if (parentJobType.getQualifiedName().contentEquals(getQualifiedJobName())) {
            mErrorReporter.abortWithError(String.format("\"%s\" may not reference itself with \"@%s\" annotation",
                    getQualifiedJobName(),
                    Shared.class.getSimpleName()),
                    mElement);
        }

        final VariableElement targetFieldElem = findTargetField(getJobTypeElement(),
                false,
                getVariableSimpleName(),
                null);

        if (targetFieldElem == null) {
            mErrorReporter.abortWithError(String.format("Missing share target field for @%s field \"%s\"",
                    Shared.class.getSimpleName(),
                    getQualifiedFieldName()),
                    mElement);

            assert false;
        }

        final FieldInfo fieldInfo;
        if (targetFieldElem.getAnnotation(JobExtra.class) != null) {
            fieldInfo = new JobExtraInfo(targetFieldElem, mProcEnv);
        } else {
            fieldInfo = new JobFlagInfo(targetFieldElem, mProcEnv);
        }

        mVariableKey = fieldInfo.getVariableKey();
        mKeyPrefix = fieldInfo.getVariableKeyPrefix();
        mIsOptional = fieldInfo.isOptional();
        mFieldAnnotationType = fieldInfo.getFieldAnnotationType();
    }

    @Override
    protected String getExpectedSuperclass() {
        return Consts.JOB_TYPE_FORK_JOIN;
    }

    @Override
    public boolean isOptional() {
        return mIsOptional;
    }

    private VariableElement findTargetField(TypeElement jobType, boolean isTargetType, String fieldName, List<String> visitedJobTypes) {
        // Resolve cyclic field reference
        if (visitedJobTypes == null) {
            visitedJobTypes = Lists.newArrayList();
        }

        if (isTargetType) {
            visitedJobTypes.add(jobType.getQualifiedName().toString());

            if (visitedJobTypes.size() > 1) {
                final int sz = visitedJobTypes.size();
                for (int index = 0; index < sz; index++) {
                    final String currentTypeName = visitedJobTypes.get(index);

                    for (int j = index + 1; j < sz; j++) {
                        if (currentTypeName.equals(visitedJobTypes.get(j))) {
                            mErrorReporter.abortWithError(
                                    String.format("Failed to resolve field \"%s\" share target. " +
                                                    "Found cyclic reference between \"%s\" and \"%s\" fields. " +
                                                    "Which one should be shared to another?",
                                            fieldName,
                                            currentTypeName,
                                            visitedJobTypes.get(j - 1)),
                                    mElement);
                        }
                    }
                }
            }
        }

        final TypeElement sharedJobType;
        if (isTargetType) {
            sharedJobType = jobType;
        } else {
            sharedJobType = getSharedJobType(jobType);
        }

        final List<VariableElement> jobFields = ElementFilter.fieldsIn(sharedJobType.getEnclosedElements());
        for (VariableElement field : jobFields) {
            if (field.getSimpleName().toString().equals(fieldName)) {
                final Shared annotation = field.getAnnotation(Shared.class);
                if (annotation != null) {
                    // Search for annotated field in next share target..
                    final TypeElement shareTargetType = extractJobType(annotation);

                    if (isSuitableShareTarget(shareTargetType)) {
                        if (!sharedJobType.equals(jobType)) {
                            // register transient share target
                            visitedJobTypes.add(sharedJobType.getQualifiedName().toString());
                        }

                        return findTargetField(shareTargetType, true, fieldName, visitedJobTypes);
                    }

                    return findTargetField(sharedJobType,
                            !sharedJobType.equals(jobType),
                            fieldName,
                            visitedJobTypes);
                }

                return field;
            }
        }

        return null;
    }

    private TypeElement getSharedJobType(TypeElement sharedJobType) {
        if (sharedJobType.equals(getJobTypeElement())) {
            return getOriginShareTarget();
        }

        final Shared jobAnnotation = sharedJobType.getAnnotation(Shared.class);

        if (jobAnnotation == null) {
            mErrorReporter.abortWithError(String.format("Unable to find share target for \"%s\". " +
                            "Neither \"%s\" has @%s job declaration nor field itself.",
                    getQualifiedFieldName(),
                    sharedJobType.getQualifiedName(),
                    Shared.class.getSimpleName()),
                    mElement);
        }

        return extractJobTypeChecked(jobAnnotation);
    }

    private TypeElement getOriginShareTarget() {
        final Shared elemAnnotation = mElement.getAnnotation(Shared.class);
        final TypeElement sharedElementType = extractJobType(elemAnnotation);

        if (sharedElementType == null || sharedElementType.getQualifiedName().contentEquals(Class.class.getName())) {
            final TypeElement jobTypeElem = getJobTypeElement();
            final Shared jobAnnotation = jobTypeElem.getAnnotation(Shared.class);
            if (jobAnnotation == null) {
                mErrorReporter.abortWithError(String.format("Unable to find share target for \"%s\". " +
                                "Neither \"%s\" has @%s job declaration nor field itself.",
                        getQualifiedFieldName(),
                        jobTypeElem.getQualifiedName(),
                        Shared.class.getSimpleName()),
                        mElement);
            }

            return extractJobTypeChecked(jobAnnotation);
        }

        return extractJobTypeChecked(elemAnnotation);
    }

    private TypeElement extractJobType(Shared annotation) {
        TypeElement refJobType;
        try {
            final Class<?> jobClass = annotation.value();
            refJobType = mProcEnv.getElementUtils().getTypeElement(jobClass.getCanonicalName());

        } catch (MirroredTypeException e) {
            refJobType = TypeSimplifier.toTypeElement(e.getTypeMirror());
        }

        return refJobType;
    }

    private TypeElement extractJobTypeChecked(Shared annotation) {
        final TypeElement sharedJobType = extractJobType(annotation);

        ensureSuitableShareTarget(sharedJobType);

        return sharedJobType;
    }

    private boolean isSuitableShareTarget(TypeElement shareTargetType) {
        if (shareTargetType == null || shareTargetType.getQualifiedName().toString().equals(Class.class.getName())) {
            return false;
        }

        checkJobSuperclass(shareTargetType);

        return true;
    }

    private void ensureSuitableShareTarget(TypeElement shareTargetType) {
        if (isSuitableShareTarget(shareTargetType)) {
            return;
        }

        mErrorReporter.abortWithError(String.format(
                "Unable to define field \"%s\" share target. Please define target job " +
                        "class using @%s annotation applied to \"%s\" or \"%s\" field.",
                getQualifiedFieldName(),
                Shared.class.getSimpleName(),
                getQualifiedJobName(),
                getVariableSimpleName()),
                mElement);
    }

    @Override
    public String getVariableKey() {
        return mVariableKey;
    }

    @Override
    public String getVariableKeyPrefix() {
        return mKeyPrefix;
    }

    @Override
    public Class<?> getFieldAnnotationType() {
        if (mFieldAnnotationType != null) {
            return mFieldAnnotationType;
        }

        return Shared.class;
    }
}