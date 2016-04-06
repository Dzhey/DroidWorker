package com.be.library.worker.annotations.compiler.statements;

import com.be.library.worker.annotations.compiler.FieldInfo;
import com.squareup.javapoet.MethodSpec;

/**
 * Generates flag setter
 *
 * Created by Dzhey on 29-Mar-16.
 */
public class ForkJoinJobFlagSetterBuilder implements MethodStatementBuilder {

    private final String mArgJobName;

    protected ForkJoinJobFlagSetterBuilder(String argJobName) {
        mArgJobName = argJobName;
    }

    public static ForkJoinJobFlagSetterBuilder of(String argJobName) {
        return new ForkJoinJobFlagSetterBuilder(argJobName);
    }

    @Override
    public void buildStatements(MethodSpec.Builder specBuilder, FieldInfo fieldInfo) {
        final String extraFieldName = fieldInfo.makeKeyFieldName();

        if (!fieldInfo.isOptional()) {
            specBuilder.beginControlFlow("if (!$L.hasFlag($L))", mArgJobName, extraFieldName);
            specBuilder.addStatement("throw new $T($S)",
                    IllegalArgumentException.class,
                    String.format("Failed to inject flag to \"%s\". Required flag \"%s\" not found in job params.",
                            fieldInfo.getSimpleJobName(),
                            fieldInfo.getVariableSimpleName()));
            specBuilder.endControlFlow();

            specBuilder.addStatement("$L.$L = $L.checkFlag($L)",
                    mArgJobName,
                    fieldInfo.getVariableSimpleName(),
                    mArgJobName,
                    extraFieldName);
        } else {
            specBuilder.beginControlFlow("if ($L.hasFlag($L))", mArgJobName, extraFieldName);
            specBuilder.addStatement("$L.$L = $L.checkFlag($L)",
                    mArgJobName,
                    fieldInfo.getVariableSimpleName(),
                    mArgJobName,
                    extraFieldName);
            specBuilder.endControlFlow();
        }
    }
}
