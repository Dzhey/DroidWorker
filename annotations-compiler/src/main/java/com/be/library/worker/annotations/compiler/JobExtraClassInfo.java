package com.be.library.worker.annotations.compiler;

import com.be.library.worker.annotations.JobExtra;
import com.google.common.base.Strings;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public class JobExtraClassInfo {

    private final VariableElement mAnnotatedElement;
    private final String mVariableName;
    private final ErrorReporter mErrorReporter;
    private final TypeMirror mBaseJobTypeMirror;
    private final Types mTypeUtils;
    private final Elements mElementUtils;
    private final String mQualifiedJobName;
    private final String mSimpleJobName;
    private final String mPackageName;

    public JobExtraClassInfo(VariableElement variableElement,
                             ProcessingEnvironment env) throws IllegalArgumentException {

        mErrorReporter = new ErrorReporter(env);
        mAnnotatedElement = variableElement;
        mElementUtils = env.getElementUtils();
        mTypeUtils = env.getTypeUtils();
        mBaseJobTypeMirror = mElementUtils.getTypeElement(Consts.BASE_JOB_TYPE_NAME).asType();

        final TypeElement jobClassElement = TypeSimplifier.enclosingClass(variableElement);
        if (mBaseJobTypeMirror == null) {
            mErrorReporter.abortWithError(
                    "failed to find " + Consts.BASE_JOB_TYPE_NAME + "class", jobClassElement);
        }

        checkJobSuperclass(jobClassElement);

        mQualifiedJobName = jobClassElement.getQualifiedName().toString();
        mSimpleJobName = jobClassElement.getSimpleName().toString();
        mPackageName = TypeSimplifier.packageNameOf(jobClassElement);

        final JobExtra annotation = variableElement.getAnnotation(JobExtra.class);

        if (!variableElement.getModifiers().contains(Modifier.DEFAULT)
                && !variableElement.getModifiers().contains(Modifier.PUBLIC)) {

            mErrorReporter.abortWithError(variableElement.getSimpleName().toString() +
                    "in " +
                    mQualifiedJobName +
                    " should have public or default visibility modifier", variableElement);
        }

        if (Strings.isNullOrEmpty(annotation.value())) {
            mVariableName = JobProcessor.ANNOTATION_PRINTABLE +
                    "_" +
                    mQualifiedJobName +
                    "_" +
                    variableElement.asType().toString() +
                    "_" +
                    variableElement.getSimpleName().toString();
        } else {
            mVariableName = annotation.value();
        }
    }

    public String getQualifiedJobName() {
        return mQualifiedJobName;
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
                    "%s may only present in job class",
                    JobProcessor.ANNOTATION_PRINTABLE), jobClassElement);
        }
    }

    private boolean checkJobSuperclassImpl(TypeMirror parent) {
        for (TypeMirror type : mTypeUtils.directSupertypes(parent)) {
            if (mTypeUtils.isAssignable(type, mBaseJobTypeMirror)) {
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