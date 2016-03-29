package com.be.library.worker.annotations.compiler;

import com.be.library.worker.annotations.JobExtra;
import com.be.library.worker.annotations.compiler.statements.AddExtraStatementBuilder;
import com.be.library.worker.annotations.compiler.statements.ForkJoinJobExtraSetterBuilder;
import com.be.library.worker.annotations.compiler.statements.ForkJoinJobFlagSetterBuilder;
import com.be.library.worker.annotations.compiler.statements.JobExtraSetterBuilder;
import com.be.library.worker.annotations.compiler.statements.JobFlagSetterBuilder;
import com.be.library.worker.annotations.compiler.statements.SetFlagStatementBuilder;
import com.google.common.collect.Iterables;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;

/**
 * Created by dzhey on 25/03/16.
 */
public class JobExtraInjectorGenerator {

    private static final String SUFFIX = "Extras";
    private static final String ARG_JOB = "job";
    private static final String VAR_PARAMS = "params";

    private ProcessingEnvironment mProcessingEnvironment;
    private final Logger mLogger;

    public JobExtraInjectorGenerator(ProcessingEnvironment environment) {
        mProcessingEnvironment = environment;
        mLogger = new Logger(mProcessingEnvironment);
    }

    public void generateCode(JobClassInfo info) throws IOException {
        for (Map.Entry<String, Collection<FieldInfo>> entry : info.getClassesInfo().entrySet()) {
            generateInjector(entry.getKey(), entry.getValue());
        }
    }

    private void generateInjector(String qualifiedJobName, Collection<FieldInfo> extras) throws IOException {
        final FieldInfo firstEntry = Iterables.get(extras, 0);
        final String injectorClassName = firstEntry.getSimpleJobName() + SUFFIX;
        final String packageName = firstEntry.getPackageName();
        final TypeName jobTypeName = TypeVariableName.get(qualifiedJobName);

        mLogger.note(String.format("Generating extras injector for \"%s\" (%s)..",
                qualifiedJobName, injectorClassName));

        final TypeSpec.Builder classSpecBuilder = TypeSpec.classBuilder(injectorClassName)
                .addModifiers(Modifier.FINAL, Modifier.PUBLIC)
                .addMethod(makeCaptureExtrasMethodSpec(extras))
                .addMethod(makeInjectExtrasMethodSpec(jobTypeName, extras));

        for (FieldInfo info : extras) {
            classSpecBuilder.addField(FieldSpec.builder(String.class,
                    info.makeKeyFieldName(),
                    Modifier.PUBLIC,
                    Modifier.STATIC,
                    Modifier.FINAL)
                    .initializer("$S", info.getVariableKey())
                    .build());
        }

        final JavaFile javaFile = JavaFile.builder(packageName, classSpecBuilder.build())
                .build();

        javaFile.writeTo(mProcessingEnvironment.getFiler());
    }

    private MethodSpec makeCaptureExtrasMethodSpec(Collection<FieldInfo> extrasInfo) {
        final TypeName delegateTypeName = TypeVariableName.get(Consts.JOB_CONFIGURATOR_DELEGATE);
        final MethodSpec.Builder captureExtrasSpecBuilder = MethodSpec.methodBuilder("captureExtras")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(delegateTypeName);

        for (FieldInfo info : extrasInfo) {
            captureExtrasSpecBuilder.addParameter(
                    TypeVariableName.get(info.getVariableTypeName()),
                    info.getVariableSimpleName(),
                    Modifier.FINAL);
        }

        final String configuratorArgName = "configurator";
        final MethodSpec.Builder delegateMethodSpecBuilder = MethodSpec.methodBuilder("configure")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeVariableName.get(Consts.JOB_CONFIGURATOR), configuratorArgName)
                .returns(void.class);

        for (FieldInfo info : extrasInfo) {
            if (info.getFieldAnnotationType().equals(JobExtra.class)) {
                AddExtraStatementBuilder.of(configuratorArgName)
                        .buildStatements(delegateMethodSpecBuilder, info);
            } else {
                SetFlagStatementBuilder.of(configuratorArgName)
                        .buildStatements(delegateMethodSpecBuilder, info);
            }
        }

        final TypeSpec.Builder delegateSpecBuilder = TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(TypeVariableName.get(Consts.JOB_CONFIGURATOR_DELEGATE))
                .addMethod(delegateMethodSpecBuilder.build());

        captureExtrasSpecBuilder.addStatement("return $L", delegateSpecBuilder.build());


        return captureExtrasSpecBuilder.build();
    }

    private MethodSpec makeInjectExtrasMethodSpec(TypeName jobTypeName, Collection<FieldInfo> extrasInfo) {
        if (Iterables.get(extrasInfo, 0).isForkJoinJob()) {
            return makeForkJoinInjectExtrasMethodSpec(jobTypeName, extrasInfo);
        }

        final TypeName jobParamsTypeName = TypeVariableName.get(Consts.JOB_PARAMS_TYPE);

        final MethodSpec.Builder injectParamsSpecBuilder = MethodSpec.methodBuilder("injectExtras")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(jobTypeName, ARG_JOB)
                .returns(void.class);

        injectParamsSpecBuilder.addStatement("final $T $L = $L.getParams()",
                jobParamsTypeName, VAR_PARAMS, ARG_JOB);

        for (FieldInfo info : extrasInfo) {
            if (info.getFieldAnnotationType().equals(JobExtra.class)) {
                JobExtraSetterBuilder.of(ARG_JOB, VAR_PARAMS)
                        .buildStatements(injectParamsSpecBuilder, info);
            } else {
                JobFlagSetterBuilder.of(ARG_JOB, VAR_PARAMS)
                        .buildStatements(injectParamsSpecBuilder, info);
            }
        }

        return injectParamsSpecBuilder.build();
    }

    private MethodSpec makeForkJoinInjectExtrasMethodSpec(
            TypeName jobTypeName, Collection<FieldInfo> extrasInfo) {

        final MethodSpec.Builder injectParamsSpecBuilder = MethodSpec.methodBuilder("injectExtras")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(jobTypeName, ARG_JOB)
                .returns(void.class);

        for (FieldInfo info : extrasInfo) {
            if (info.getFieldAnnotationType().equals(JobExtra.class)) {
                ForkJoinJobExtraSetterBuilder.of(ARG_JOB)
                        .buildStatements(injectParamsSpecBuilder, info);
            } else {
                ForkJoinJobFlagSetterBuilder.of(ARG_JOB)
                        .buildStatements(injectParamsSpecBuilder, info);
            }
        }

        return injectParamsSpecBuilder.build();
    }
}
