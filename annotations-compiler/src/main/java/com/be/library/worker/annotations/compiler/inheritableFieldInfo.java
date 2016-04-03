package com.be.library.worker.annotations.compiler;

import com.be.library.worker.annotations.Inherited;
import com.be.library.worker.annotations.JobExtra;
import com.google.common.collect.Lists;

import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.util.ElementFilter;

public abstract class InheritableFieldInfo extends FieldInfo {

    private final ProcessingEnvironment mProcEnv;
    private final ErrorReporter mErrorReporter;
    private String mVariableKey;
    private String mKeyPrefix;
    private boolean mIsOptional;

    public InheritableFieldInfo(VariableElement variableElement,
                                ProcessingEnvironment env) throws IllegalArgumentException {

        super(variableElement, env);

        mProcEnv = env;
        mErrorReporter = new ErrorReporter(env);
    }

    @Override
    public void init() {
        super.init();

        if (!isInherited()) {
            return;
        }

        final TypeElement parentJobType = getParentJobType(getJobTypeElement());
        if (parentJobType.getQualifiedName().contentEquals(getQualifiedJobName())) {
            mErrorReporter.abortWithError(String.format("\"%s\" may not reference itself with \"@%s\" annotation",
                    getQualifiedJobName(),
                    Inherited.class.getSimpleName()));
        }

        final VariableElement parentFieldElem = findParentField(getJobTypeElement(),
                getVariableSimpleName(),
                null);

        if (parentFieldElem == null) {
            mErrorReporter.abortWithError(String.format("Missing parent field for @%s field \"%s\"",
                    Inherited.class.getSimpleName(),
                    getQualifiedFieldName()));
            assert false;
        }

        final FieldInfo fieldInfo;
        if (parentFieldElem.getAnnotation(JobExtra.class) != null) {
            fieldInfo = new JobExtraInfo(parentFieldElem, mProcEnv);
        } else {
            fieldInfo = new JobFlagInfo(parentFieldElem, mProcEnv);
        }

        mVariableKey = fieldInfo.getVariableKey();
        mKeyPrefix = fieldInfo.getVariableKeyPrefix();
        mIsOptional = fieldInfo.isOptional();
    }

    @Override
    protected String getExpectedSuperclass() {
        return Consts.JOB_TYPE_FORK_JOIN;
    }

    @Override
    public boolean isOptional() {
        return mIsOptional;
    }

    private VariableElement findParentField(TypeElement jobType, String fieldName, List<String> visitedJobTypes) {
        // Resolve cyclic field inheritance
        if (visitedJobTypes == null) {
            visitedJobTypes = Lists.newArrayList();
        }
        visitedJobTypes.add(jobType.getQualifiedName().toString());
        if (visitedJobTypes.size() > 1) {
            final int sz = visitedJobTypes.size();
            for (int index = 0; index < sz; index++) {
                final String currentTypeName = visitedJobTypes.get(index);

                for (int j = index + 1; j < sz; j++) {
                    if (currentTypeName.equals(visitedJobTypes.get(j))) {
                        mErrorReporter.abortWithError(
                                String.format("Failed to resolve field \"%s\" inheritance. " +
                                                "Found cyclic reference between \"%s\" and \"%s\" fields. " +
                                                "Which one should inherit another?",
                                        fieldName,
                                        currentTypeName,
                                        visitedJobTypes.get(j - 1)));
                    }
                }
            }
        }

        final TypeElement parentJobType = getParentJobType(jobType);
        final List<VariableElement> jobFields = ElementFilter.fieldsIn(parentJobType.getEnclosedElements());
        for (VariableElement field : jobFields) {
            if (field.getSimpleName().toString().equals(fieldName)) {
                if (field.getAnnotation(Inherited.class) != null) {
                    // Search for annotated field in next parent..
                    return findParentField(parentJobType, fieldName, visitedJobTypes);
                }

                return field;
            }
        }

        return null;
    }

    private TypeElement getParentJobType(TypeElement inheritorJobType) {
        final Inherited annotation = inheritorJobType.getAnnotation(Inherited.class);
        if (annotation == null) {
            mErrorReporter.abortWithError(String.format("Missing @%s annotation in \"%s\" declaration",
                    Inherited.class.getSimpleName(),
                    inheritorJobType.getQualifiedName()));
            assert false;
        }

        return extractJobType(annotation);
    }

    private TypeElement extractJobType(Inherited annotation) {
        TypeElement parentJobType;
        try {
            final Class<?> jobClass = annotation.value();
            parentJobType = mProcEnv.getElementUtils().getTypeElement(jobClass.getCanonicalName());

        } catch (MirroredTypeException e) {
            parentJobType = TypeSimplifier.toTypeElement(e.getTypeMirror());
        }

        if (parentJobType == null || parentJobType.getQualifiedName().toString().equals(Class.class.getName())) {
            mErrorReporter.abortWithError(String.format(
                    "Unable to define parent for the field \"%s\". " +
                            "Please define parent job class using @%s annotation applied to \"%s\".",
                    getQualifiedFieldName(),
                    Inherited.class.getSimpleName(),
                    getQualifiedJobName()));
        }

        checkJobSuperclass(parentJobType);

        return parentJobType;
    }

    @Override
    public String getVariableKey() {
        return mVariableKey;
    }

    @Override
    public String getVariableKeyPrefix() {
        return mKeyPrefix;
    }
}