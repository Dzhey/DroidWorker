package com.be.library.worker.annotations.compiler.statements;

import com.be.library.worker.annotations.compiler.FieldInfo;
import com.squareup.javapoet.MethodSpec;

/**
 * Generates flag setter
 *
 * Created by Dzhey on 29-Mar-16.
 */
public class JobFlagSetterBuilder implements MethodStatementBuilder {

    private final String mArgJobName;
    private final String mVarJobParamsName;

    protected JobFlagSetterBuilder(String argJobName, String varJobParamsName) {
        mArgJobName = argJobName;
        mVarJobParamsName = varJobParamsName;
    }

    public static JobFlagSetterBuilder of(String argJobName, String varJobParamsName) {
        return new JobFlagSetterBuilder(argJobName, varJobParamsName);
    }

    @Override
    public void buildStatements(MethodSpec.Builder specBuilder, FieldInfo fieldInfo) {
        final String extraFieldName = fieldInfo.makeKeyFieldName();

        if (!fieldInfo.isOptional()) {
            specBuilder.beginControlFlow("if (!$L.hasFlag($L))",
                    mVarJobParamsName, extraFieldName);
            specBuilder.addStatement("throw new $T($S)",
                    IllegalArgumentException.class,
                    String.format("Failed to inject flag to \"%s\". Required flag \"%s\" not found in job params.",
                            fieldInfo.getSimpleJobName(),
                            fieldInfo.getVariableSimpleName()));
            specBuilder.endControlFlow();
        }

        specBuilder.addStatement("$L.$L = $L.checkFlag($L)",
                mArgJobName,
                fieldInfo.getVariableSimpleName(),
                mVarJobParamsName,
                extraFieldName);
    }
}
