package com.be.library.worker.annotations.compiler.statements;

import com.be.library.worker.annotations.compiler.FieldInfo;
import com.squareup.javapoet.MethodSpec;

/**
 * Generates addExtra() for @JobExtra-annotated fields
 * Created by Dzhey on 29-Mar-16.
 */
public class AddExtraStatementBuilder implements MethodStatementBuilder {

    private final String mArgConfigurator;

    protected AddExtraStatementBuilder(String argConfigurator) {
        mArgConfigurator = argConfigurator;
    }

    public static AddExtraStatementBuilder of(String argConfiguratorName) {
        return new AddExtraStatementBuilder(argConfiguratorName);
    }

    @Override
    public void buildStatements(MethodSpec.Builder specBuilder, FieldInfo fieldInfo) {
        specBuilder.addStatement( "$L.addExtra($L, $L)",
                mArgConfigurator,
                fieldInfo.makeKeyFieldName(),
                fieldInfo.getVariableSimpleNameWithoutPrefix());
    }
}
