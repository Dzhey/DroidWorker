package com.be.library.worker.annotations.compiler.statements;

import com.be.library.worker.annotations.compiler.FieldInfo;
import com.squareup.javapoet.MethodSpec;

/**
 * Generates flag() call for @JobFlag-annotated fields
 * Created by Dzhey on 29-Mar-16.
 */
public class SetFlagStatementBuilder implements MethodStatementBuilder {

    private final String mArgConfigurator;

    protected SetFlagStatementBuilder(String argConfigurator) {
        mArgConfigurator = argConfigurator;
    }

    public static SetFlagStatementBuilder of(String argConfiguratorName) {
        return new SetFlagStatementBuilder(argConfiguratorName);
    }

    @Override
    public void buildStatements(MethodSpec.Builder specBuilder, FieldInfo fieldInfo) {
        specBuilder.addStatement( "$L.flag($L, $L)",
                mArgConfigurator,
                fieldInfo.makeKeyFieldName(),
                fieldInfo.getVariableSimpleNameWithoutPrefix());
    }
}
