package com.be.library.worker.annotations.compiler;

import com.be.library.worker.annotations.Inherited;
import com.google.common.base.CaseFormat;
import com.google.common.collect.Lists;

import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
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

    public String makeKeyFieldName() {
        String varName = getVariableSimpleName();

        if (varName.length() > 0 && varName.toUpperCase().startsWith("M")) {
            varName = varName.substring(1, varName.length());
        }

        return getVariableKeyPrefix() +
                CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, varName);
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

        final TypeElement parentJobType = extractParentJobType(jobType);
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

    private TypeElement extractParentJobType(TypeElement inheritorJobType) {
        final Inherited jobAnnotation = inheritorJobType.getAnnotation(Inherited.class);
        if (jobAnnotation == null) {
            mErrorReporter.abortWithError(String.format("Missing @%s annotation in \"%s\" declaration",
                    Inherited.class.getSimpleName(),
                    inheritorJobType.getQualifiedName()));
            assert false;
        }

        return extractJobType(jobAnnotation);
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

