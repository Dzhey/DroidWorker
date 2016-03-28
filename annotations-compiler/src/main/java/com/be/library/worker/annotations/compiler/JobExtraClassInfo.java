package com.be.library.worker.annotations.compiler;

import com.be.library.worker.annotations.JobExtra;
import com.google.common.base.Strings;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

public class JobExtraClassInfo {

    private final String mVariableKey;
    private final String mVariableName;
    private final ErrorReporter mErrorReporter;
    private final Types mTypeUtils;
    private final String mQualifiedJobName;
    private final String mSimpleJobName;
    private final String mPackageName;
    private final String mVariableType;
    private final boolean mIsForkJoinJob;
    private final boolean mIsOptional;

    public JobExtraClassInfo(VariableElement variableElement,
                             ProcessingEnvironment env) throws IllegalArgumentException {

        mErrorReporter = new ErrorReporter(env);
        mTypeUtils = env.getTypeUtils();

        final TypeElement jobClassElement = TypeSimplifier.enclosingClass(variableElement);

        checkJobSuperclass(jobClassElement);

        mIsForkJoinJob = isForkJoinJob(jobClassElement);
        mQualifiedJobName = jobClassElement.getQualifiedName().toString();
        mSimpleJobName = jobClassElement.getSimpleName().toString();
        mPackageName = TypeSimplifier.packageNameOf(jobClassElement);

        final JobExtra annotation = variableElement.getAnnotation(JobExtra.class);

        mIsOptional = annotation.optional();

        if (variableElement.getModifiers().contains(Modifier.FINAL)) {
            mErrorReporter.abortWithError("\"" +
                    variableElement.getSimpleName().toString() +
                    "\" in \"" +
                    mQualifiedJobName +
                    "\" cannot be final", variableElement);
        }

        if (variableElement.getModifiers().contains(Modifier.PROTECTED)
                || variableElement.getModifiers().contains(Modifier.PRIVATE)
                || variableElement.getModifiers().contains(Modifier.STATIC)) {

            mErrorReporter.abortWithError("\"" +
                    variableElement.getSimpleName().toString() +
                    "\" in \"" +
                    mQualifiedJobName +
                    "\" should have public or default visibility modifier", variableElement);
        }

        mVariableType = variableElement.asType().toString();
        mVariableName = variableElement.getSimpleName().toString();

        if (Strings.isNullOrEmpty(annotation.value())) {
            mVariableKey = JobProcessor.ANNOTATION_PRINTABLE +
                    "_" +
                    mSimpleJobName +
                    "_" +
                    mVariableName;
        } else {
            mVariableKey = annotation.value();
        }
    }

    private boolean isForkJoinJob(TypeElement jobClassElement) {
        TypeMirror superType = jobClassElement.getSuperclass();

        while (true) {
            if (!superType.getKind().equals(TypeKind.DECLARED)) {
                break;
            }

            final TypeElement typeElement = (TypeElement) ((DeclaredType) superType).asElement();

            if (typeElement.getQualifiedName().contentEquals("com.be.android.library.worker.base.ForkJoinJob")) {
                return true;
            }

            superType = typeElement.getSuperclass();
        }

        return false;
    }

    public String getQualifiedJobName() {
        return mQualifiedJobName;
    }

    public String getVariableKey() {
        return mVariableKey;
    }

    public String getVariableName() {
        return mVariableName;
    }

    private void checkJobSuperclass(TypeElement jobClassElement) {
        final TypeMirror parent = jobClassElement.asType();

        if (!parent.getKind().equals(TypeKind.DECLARED)) {
            mErrorReporter.abortWithError(String.format(
                    "%s may only present in job class",
                    JobProcessor.ANNOTATION_PRINTABLE), jobClassElement);
        }

        final TypeElement parentTypeElement = (TypeElement) ((DeclaredType) parent).asElement();
        if (!checkJobSuperclassImpl(parent)) {
            mErrorReporter.abortWithError(String.format(
                    "%s should implement '%s' interface in order to user annotation '%s'",
                    parentTypeElement.getQualifiedName(),
                    Consts.JOB_INTERFACE_TYPE_NAME,
                    JobProcessor.ANNOTATION_PRINTABLE), jobClassElement);
        }
    }

    private boolean checkJobSuperclassImpl(TypeMirror child) {
        for (TypeMirror type : mTypeUtils.directSupertypes(child)) {
            if (!type.getKind().equals(TypeKind.DECLARED)) {
                return false;
            }

            final String qualifiedName = ((TypeElement) ((DeclaredType) type).asElement())
                    .getQualifiedName()
                    .toString();

            if (Consts.JOB_INTERFACE_TYPE_NAME.equals(qualifiedName)) {
                return true;
            }

            if (checkJobSuperclassImpl(type)) {
                return true;
            }
        }

        return false;
    }

    public String getSimpleJobName() {
        return mSimpleJobName;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public String getVariableType() {
        return mVariableType;
    }

    public boolean isForkJoinJob() {
        return mIsForkJoinJob;
    }

    public boolean isOptional() {
        return mIsOptional;
    }
}