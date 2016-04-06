package com.be.library.worker.annotations.compiler;

import com.be.library.worker.annotations.Inherited;
import com.google.common.base.CaseFormat;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

public abstract class FieldInfo {

    private final VariableElement mElement;
    private final ProcessingEnvironment mEnvironment;
    private final TypeElement mJobTypeElement;
    private final ErrorReporter mErrorReporter;
    private final Types mTypeUtils;

    public FieldInfo(VariableElement variableElement,
                       ProcessingEnvironment env) throws IllegalArgumentException {

        mErrorReporter = new ErrorReporter(env);
        mTypeUtils = env.getTypeUtils();
        mElement = variableElement;
        mJobTypeElement = TypeSimplifier.enclosingClass(variableElement);
        mEnvironment = env;

        if (variableElement.getModifiers().contains(Modifier.FINAL)) {
            mErrorReporter.abortWithError("\"" +
                    variableElement.getSimpleName().toString() +
                    "\" in \"" +
                    getQualifiedJobName() +
                    "\" cannot be final", variableElement);
        }

        if (variableElement.getModifiers().contains(Modifier.PROTECTED)
                || variableElement.getModifiers().contains(Modifier.PRIVATE)
                || variableElement.getModifiers().contains(Modifier.STATIC)) {

            mErrorReporter.abortWithError("\"" +
                    variableElement.getSimpleName().toString() +
                    "\" in \"" +
                    getQualifiedJobName() +
                    "\" should have public or default visibility modifier", variableElement);
        }
    }

    public void init() {
        checkJobSuperclass(mJobTypeElement);
    }

    public abstract String getVariableKey();

    public abstract boolean isOptional();

    public abstract String getVariableKeyPrefix();

    public abstract Class<?> getFieldAnnotationType();

    public abstract boolean isInherited();

    public String getVariableSimpleNameWithoutPrefix() {
        final String varName = getVariableSimpleNameWithoutPrefixImpl();

        if (varName.length() > 1 && Character.isUpperCase(varName.charAt(0))) {
            return varName.substring(0, 1).toLowerCase() +
                    varName.substring(1, varName.length());
        }

        return varName;
    }

    private String getVariableSimpleNameWithoutPrefixImpl() {
        String varName = getVariableSimpleName();

        if (varName.length() < 2) {
            return varName;
        }

        if (Character.isLowerCase(varName.charAt(0)) && Character.isUpperCase(varName.charAt(1))) {
            return varName.substring(1, varName.length());
        }

        return varName;
    }

    public String makeKeyFieldName() {
        String varName = getVariableSimpleNameWithoutPrefixImpl();

        return getVariableKeyPrefix() +
                CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, varName);
    }

    public String makeVariableSetterName() {
        final String varName = getVariableSimpleNameWithoutPrefixImpl();

        if (varName.length() < 2) {
            return "set" + varName.toUpperCase();
        }

        return "set" +
                varName.substring(0, 1).toUpperCase() +
                varName.substring(1, varName.length());
    }

    public String getVariableSimpleName() {
        return mElement.getSimpleName().toString();
    }

    public String getVariableTypeName() {
        return mElement.asType().toString();
    }

    public VariableElement getElement() {
        return mElement;
    }

    public ProcessingEnvironment getEnvironment() {
        return mEnvironment;
    }

    public String getQualifiedJobName() {
        return mJobTypeElement.getQualifiedName().toString();
    }

    public String getSimpleJobName() {
        return mJobTypeElement.getSimpleName().toString();
    }

    public String getPackageName() {
        return TypeSimplifier.packageNameOf(mJobTypeElement);
    }

    public String getQualifiedFieldName() {
        return getQualifiedJobName() + "." + getVariableSimpleName();
    }

    public TypeElement getJobTypeElement() {
        return mJobTypeElement;
    }

    public boolean isForkJoinJob() {
        TypeMirror superType = mJobTypeElement.getSuperclass();

        while (true) {
            if (!superType.getKind().equals(TypeKind.DECLARED)) {
                break;
            }

            final TypeElement typeElement = (TypeElement) ((DeclaredType) superType).asElement();

            if (typeElement.getQualifiedName().contentEquals(Consts.JOB_TYPE_FORK_JOIN)) {
                return true;
            }

            superType = typeElement.getSuperclass();
        }

        return false;
    }

    protected void checkJobSuperclass(TypeElement jobClassElement) {
        final TypeMirror parent = jobClassElement.asType();

        if (!parent.getKind().equals(TypeKind.DECLARED)) {
            mErrorReporter.abortWithError(String.format(
                    "%s may only present in job class",
                    JobProcessor.EXTRA_ANNOTATION_PRINTABLE), jobClassElement);
        }

        final TypeElement parentTypeElement = TypeSimplifier.toTypeElement(parent);
        if (!checkJobSuperclassImpl(parent)) {
            mErrorReporter.abortWithError(String.format(
                    "%s should implement '%s' interface in order to use annotation '@%s'",
                    parentTypeElement.getQualifiedName(),
                    getExpectedSuperclass(),
                    getFieldAnnotationType().getSimpleName()), jobClassElement);
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

            if (getExpectedSuperclass().equals(qualifiedName)) {
                return true;
            }

            if (checkJobSuperclassImpl(type)) {
                return true;
            }
        }

        return false;
    }

    protected String getExpectedSuperclass() {
        if (isInherited()) {
            return Consts.JOB_TYPE_FORK_JOIN;
        }

        return Consts.JOB_INTERFACE_TYPE_NAME;
    }

    private TypeElement extractJobType(Inherited annotation) {
        TypeElement parentJobType;
        try {
            final Class<?> jobClass = annotation.value();
            parentJobType = mEnvironment.getElementUtils().getTypeElement(jobClass.getCanonicalName());

        } catch (MirroredTypeException e) {
            parentJobType = TypeSimplifier.toTypeElement(e.getTypeMirror());
        }

        if (parentJobType == null || parentJobType.getQualifiedName().toString().equals(Class.class.getName())) {
            mErrorReporter.abortWithError(String.format(
                    "unable to define parent for the field \"%s\"", getQualifiedFieldName()));
        }

        checkJobSuperclass(parentJobType);

        return parentJobType;
    }
}

