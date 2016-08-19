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
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
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
    private static final String TYPE_NAME_CONFIGURATOR_BUILDER = "ConfiguratorBuilder";

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
                .addMethod(makeCaptureExtrasMethodSpec(extras));

        for (FieldInfo info : extras) {
            classSpecBuilder.addField(FieldSpec.builder(String.class,
                    info.makeKeyFieldName(),
                    Modifier.PUBLIC,
                    Modifier.STATIC,
                    Modifier.FINAL)
                    .initializer("$S", info.getVariableKey())
                    .build());
        }

        final TypeSpec builderTypeSpec = generateBuilderTypeSpec(extras);

        classSpecBuilder.addType(builderTypeSpec);
        final TypeName builderTypeName = TypeVariableName.get(TYPE_NAME_CONFIGURATOR_BUILDER);
        classSpecBuilder.addMethod(MethodSpec.methodBuilder("captureExtras")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addStatement("return new $T()", builderTypeName)
                .returns(builderTypeName)
                .build());

        classSpecBuilder.addMethod(makeInjectExtrasMethodSpec(jobTypeName, extras));

        final JavaFile javaFile = JavaFile.builder(packageName, classSpecBuilder.build())
                .build();

        javaFile.writeTo(mProcessingEnvironment.getFiler());
    }

    private TypeSpec generateBuilderTypeSpec(Collection<FieldInfo> extrasInfo) {
        final TypeName builderTypeName = TypeVariableName.get(TYPE_NAME_CONFIGURATOR_BUILDER);
        final TypeSpec.Builder captureClassSpecBuilder = TypeSpec.classBuilder(TYPE_NAME_CONFIGURATOR_BUILDER)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PRIVATE)
                        .build());
        final TypeName mapType = ParameterizedTypeName.get(Map.class, String.class, Object.class);
        final String mapName = "mValues";
        final TypeName hashMapType = ParameterizedTypeName.get(HashMap.class, String.class, Object.class);
        captureClassSpecBuilder.addField(
                FieldSpec.builder(mapType, mapName, Modifier.FINAL, Modifier.PRIVATE)
                        .initializer("new $T($L)", hashMapType, extrasInfo.size())
                        .build());

        for (FieldInfo info : extrasInfo) {
            captureClassSpecBuilder.addMethod(MethodSpec.methodBuilder(info.makeVariableSetterName())
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(TypeVariableName.get(info.getVariableTypeName()), info.getVariableSimpleNameWithoutPrefix())
                    .addStatement("$L.put($L, $L)", mapName, info.makeKeyFieldName(), info.getVariableSimpleNameWithoutPrefix())
                    .addStatement("return this")
                    .returns(builderTypeName)
                    .build());
        }

        final TypeName delegateTypeName = TypeVariableName.get(Consts.JOB_CONFIGURATOR_DELEGATE);
        final MethodSpec.Builder applySpecBuilder = MethodSpec.methodBuilder("apply")
                .addModifiers(Modifier.PUBLIC)
                .returns(delegateTypeName);

        for (FieldInfo info : extrasInfo) {
            if (!info.isOptional()) {
                final String fieldName = info.makeKeyFieldName();
                applySpecBuilder.beginControlFlow("if (!$L.containsKey($L))", mapName, fieldName);
                applySpecBuilder.addStatement("throw new $T($S)",
                        IllegalStateException.class,
                        String.format("required field \"%s\" value wasn't captured", info.getVariableSimpleName()));
                applySpecBuilder.endControlFlow();
            }
        }

        final String configuratorArgName = "configurator";
        final MethodSpec.Builder delegateMethodSpecBuilder = MethodSpec.methodBuilder("configure")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeVariableName.get(Consts.JOB_CONFIGURATOR), configuratorArgName)
                .returns(void.class);

        for (FieldInfo info : extrasInfo) {
            final String fieldName = info.makeKeyFieldName();

            if (info.isOptional()) {
                delegateMethodSpecBuilder.beginControlFlow("if ($L.containsKey($L))", mapName, fieldName);
            }

            if (info.getFieldAnnotationType().equals(JobExtra.class)) {
                delegateMethodSpecBuilder.addStatement("$L.addExtra($L, $L.get($L))",
                        configuratorArgName, fieldName, mapName, fieldName);
            } else {
                delegateMethodSpecBuilder.addStatement("$L.flag($L, ($T) $L.get($L))",
                        configuratorArgName, fieldName, Boolean.class, mapName, fieldName);
            }

            if (info.isOptional()) {
                delegateMethodSpecBuilder.endControlFlow();
            }
        }

        final TypeSpec.Builder delegateSpecBuilder = TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(TypeVariableName.get(Consts.JOB_CONFIGURATOR_DELEGATE))
                .addMethod(delegateMethodSpecBuilder.build());

        applySpecBuilder.addStatement("return $L", delegateSpecBuilder.build());
        captureClassSpecBuilder.addMethod(applySpecBuilder.build());

        return captureClassSpecBuilder.build();
    }

    private MethodSpec makeCaptureExtrasMethodSpec(Collection<FieldInfo> extrasInfo) {
        final TypeName delegateTypeName = TypeVariableName.get(Consts.JOB_CONFIGURATOR_DELEGATE);
        final MethodSpec.Builder captureExtrasSpecBuilder = MethodSpec.methodBuilder("captureExtras")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(delegateTypeName);

        for (FieldInfo info : extrasInfo) {
            captureExtrasSpecBuilder.addParameter(
                    TypeVariableName.get(info.getVariableTypeName()),
                    info.getVariableSimpleNameWithoutPrefix(),
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
