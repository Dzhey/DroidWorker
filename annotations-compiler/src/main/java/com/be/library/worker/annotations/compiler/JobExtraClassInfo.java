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
    private final boolean mHasDeclaredVariableKey;

    public JobExtraClassInfo(VariableElement variableElement,
                             ProcessingEnvironment env) throws IllegalArgumentException {

        mErrorReporter = new ErrorReporter(env);
        mTypeUtils = env.getTypeUtils();
//        mBaseJobTypeMirror = mElementUtils.getTypeElement(Job.class.getSimpleName()).asType();

        final TypeElement jobClassElement = TypeSimplifier.enclosingClass(variableElement);
//        if (mBaseJobTypeMirror == null) {
//            mErrorReporter.abortWithError(
//                    "failed to find " + Consts.BASE_JOB_TYPE_NAME + "class", jobClassElement);
//        }

//        checkJobSuperclass(jobClassElement);

        mQualifiedJobName = jobClassElement.getQualifiedName().toString();
        mSimpleJobName = jobClassElement.getSimpleName().toString();
        mPackageName = TypeSimplifier.packageNameOf(jobClassElement);

        final JobExtra annotation = variableElement.getAnnotation(JobExtra.class);

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

        mVariableType = ((TypeElement) ((DeclaredType) variableElement.asType()).asElement())
                .getQualifiedName()
                .toString();
        mVariableName = variableElement.getSimpleName().toString();

        if (Strings.isNullOrEmpty(annotation.value())) {
            mVariableKey = JobProcessor.ANNOTATION_PRINTABLE +
                    "_" +
                    mSimpleJobName +
                    "_" +
                    mVariableName;
            mHasDeclaredVariableKey = false;
        } else {
            mVariableKey = annotation.value();
            mHasDeclaredVariableKey = true;
        }
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

//        if (!parent.getKind().equals(TypeKind.DECLARED)) {
//            mErrorReporter.abortWithError(String.format(
//                    "%s may only present in job class",
//                    JobProcessor.ANNOTATION_PRINTABLE), jobClassElement);
//        }


        if (!checkJobSuperclassImpl(parent)) {
            mErrorReporter.abortWithError(String.format(
                    "%s should implement '%s' interface",
                    JobProcessor.ANNOTATION_PRINTABLE,
                    Consts.JOB_INTERFACE_TYPE_NAME), jobClassElement);
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

    public boolean isHasDeclaredVariableKey() {
        return mHasDeclaredVariableKey;
    }


//    private boolean checkJobSuperclassImpl(TypeMirror parent) {
//        if (mTypeUtils.isSameType(parent, mJavaObjectTypeMirror)) {
//            return false;
//        }
//
//        for (TypeMirror type : mTypeUtils.directSupertypes(parent)) {
//            if (type.getKind().equals(TypeKind.DECLARED)) {
//                continue;
//            }
//
//            final DeclaredType declaredType = (DeclaredType) type;
//            final Element element = declaredType.asElement();
//
//            if (!element.getKind().equals(ElementKind.CLASS)) {
//                continue;
//            }
//
//            if (type.get)
//        }
//
//        return false;
//    }
}