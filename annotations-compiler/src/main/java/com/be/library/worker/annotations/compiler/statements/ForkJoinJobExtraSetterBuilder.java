package com.be.library.worker.annotations.compiler.statements;

import com.be.library.worker.annotations.compiler.FieldInfo;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;

/**
 * Generates extra setter for ForkJoinJob
 *
 * Created by Dzhey on 29-Mar-16.
 */
public class ForkJoinJobExtraSetterBuilder implements MethodStatementBuilder {

    private final String mArgJobName;

    protected ForkJoinJobExtraSetterBuilder(String argJobName) {
        mArgJobName = argJobName;
    }

    public static ForkJoinJobExtraSetterBuilder of(String argJobName) {
        return new ForkJoinJobExtraSetterBuilder(argJobName);
    }

    @Override
    public void buildStatements(MethodSpec.Builder specBuilder, FieldInfo fieldInfo) {
        final String extraFieldName = fieldInfo.makeKeyFieldName();

        specBuilder.beginControlFlow("if ($L.hasExtra($L))",
                mArgJobName, extraFieldName);

        final TypeName extraParamTypeName = TypeVariableName.get(fieldInfo.getVariableTypeName());

        specBuilder.beginControlFlow("try");
        specBuilder.addStatement("$L.$L = ($T) $L.findExtra($L)",
                mArgJobName,
                fieldInfo.getVariableSimpleName(),
                extraParamTypeName,
                mArgJobName,
                extraFieldName);
        specBuilder.endControlFlow();
        specBuilder.beginControlFlow("catch ($T e)", ClassCastException.class);
        specBuilder.addStatement("throw new $T($S + \"\\\"\" + \n$L.findExtra($L).getClass().getName() + \"\\\"\")",
                RuntimeException.class,
                String.format("Failed to inject extra \"%s\" to \"%s\". Expected type \"%s\", but got ",
                        fieldInfo.getVariableSimpleName(),
                        fieldInfo.getSimpleJobName(),
                        extraParamTypeName),
                mArgJobName,
                extraFieldName);
        specBuilder.endControlFlow();
        specBuilder.endControlFlow();

        if (!fieldInfo.isOptional()) {
            specBuilder.beginControlFlow("else");
            specBuilder.addStatement("throw new $T($S)",
                    IllegalArgumentException.class,
                    String.format("Failed to inject extras to \"%s\". Required value \"%s\" not found in job params.",
                            fieldInfo.getSimpleJobName(),
                            fieldInfo.getVariableSimpleName()));
            specBuilder.endControlFlow();
        }
    }
}
