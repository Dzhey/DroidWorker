package com.be.library.worker.annotations.compiler.statements;

import com.be.library.worker.annotations.compiler.FieldInfo;
import com.squareup.javapoet.MethodSpec;

public interface MethodStatementBuilder {
    void buildStatements(MethodSpec.Builder specBuilder, FieldInfo fieldInfo);
}
