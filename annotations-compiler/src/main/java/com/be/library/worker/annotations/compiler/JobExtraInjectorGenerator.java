package com.be.library.worker.annotations.compiler;

import com.google.common.collect.Iterables;
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

    private static final String SUFFIX = "ExtrasInjector";
    private static final String ARG_JOB = "job";
    private static final String VAR_PARAMS = "params";

    private ProcessingEnvironment mProcessingEnvironment;

    public JobExtraInjectorGenerator(ProcessingEnvironment environment) {
        mProcessingEnvironment = environment;
    }

    public void generateCode(JobClassInfo info) throws IOException {
        for (Map.Entry<String, Collection<JobExtraClassInfo>> entry : info.getClassesInfo().entrySet()) {
            generateInjector(entry.getKey(), entry.getValue());
        }
    }

    private void generateInjector(String qualifiedJobName, Collection<JobExtraClassInfo> extras) throws IOException {
        final JobExtraClassInfo firstEntry = Iterables.get(extras, 0);
        final String injectorClassName = firstEntry.getSimpleJobName() + SUFFIX;
        final String packageName = firstEntry.getPackageName();
        final TypeName jobTypeName = TypeVariableName.get(qualifiedJobName);

        final TypeSpec injectorSpec = TypeSpec.classBuilder(injectorClassName)
                .addModifiers(Modifier.FINAL, Modifier.PUBLIC)
                .addMethod(makeCaptureExtrasMethodSpec(extras))
                .addMethod(makeInjectExtrasMethodSpec(jobTypeName, extras))
                .build();

        final JavaFile javaFile = JavaFile.builder(packageName, injectorSpec)
                .build();

        javaFile.writeTo(mProcessingEnvironment.getFiler());
    }

    private MethodSpec makeCaptureExtrasMethodSpec(Collection<JobExtraClassInfo> extrasInfo) {
        final MethodSpec.Builder captureExtrasSpecBuilder = MethodSpec.methodBuilder("captureExtras")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(Map.class, String.class, Object.class));

        for (JobExtraClassInfo info : extrasInfo) {
            final TypeName varTypeName = TypeVariableName.get(info.getVariableType());
            captureExtrasSpecBuilder.addParameter(varTypeName, info.getVariableName());
        }

        final String mapName = "extras";
        final TypeName mapInterfaceTypeName = ParameterizedTypeName.get(
                Map.class, String.class, Object.class);
        final TypeName mapTypeName = ParameterizedTypeName.get(
                HashMap.class, String.class, Object.class);
        captureExtrasSpecBuilder.addStatement("final $T $L = new $T($L)",
                mapInterfaceTypeName, mapName, mapTypeName, extrasInfo.size());

        for (JobExtraClassInfo info : extrasInfo) {
            captureExtrasSpecBuilder.addStatement("$L.put($S, $L)",
                    mapName,
                    info.getVariableKey(),
                    info.getVariableName());
        }

        captureExtrasSpecBuilder.addStatement("return $L", mapName);


        return captureExtrasSpecBuilder.build();
    }

    private MethodSpec makeInjectExtrasMethodSpec(TypeName jobTypeName, Collection<JobExtraClassInfo> extrasInfo) {
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

        for (JobExtraClassInfo info : extrasInfo) {
            injectParamsSpecBuilder.beginControlFlow("if ($L.hasExtra($S))",
                    VAR_PARAMS, info.getVariableKey());

            final TypeName extraParamTypeName = TypeVariableName.get(info.getVariableType());

            injectParamsSpecBuilder.beginControlFlow("try");
            injectParamsSpecBuilder.addStatement("$L.$L = ($T) $L.getExtra($S)",
                    ARG_JOB,
                    info.getVariableName(),
                    extraParamTypeName,
                    VAR_PARAMS,
                    info.getVariableKey());
            injectParamsSpecBuilder.endControlFlow();
            injectParamsSpecBuilder.beginControlFlow("catch ($T e)", ClassCastException.class);
            injectParamsSpecBuilder.addStatement("throw new $T($S + \"\\\"\" + \n$L.getExtra($S).getClass().getName() + \"\\\"\")",
                    RuntimeException.class,
                    String.format("Failed to inject extra \"%s\" to \"%s\". Expected type \"%s\", but got ",
                            info.getVariableName(),
                            info.getSimpleJobName(),
                            extraParamTypeName),
                    VAR_PARAMS,
                    info.getVariableKey());
            injectParamsSpecBuilder.endControlFlow();
            injectParamsSpecBuilder.endControlFlow();

            if (!info.isOptional()) {
                injectParamsSpecBuilder.beginControlFlow("else");
                injectParamsSpecBuilder.addStatement("throw new $T($S)",
                        IllegalArgumentException.class,
                        String.format("Failed to inject extras to \"%s\". Required value \"%s\" not found in job params.",
                                info.getSimpleJobName(),
                                info.getVariableName()));
                injectParamsSpecBuilder.endControlFlow();
            }
        }

        return injectParamsSpecBuilder.build();
    }

    private MethodSpec makeForkJoinInjectExtrasMethodSpec(
            TypeName jobTypeName, Collection<JobExtraClassInfo> extrasInfo) {

        final MethodSpec.Builder injectParamsSpecBuilder = MethodSpec.methodBuilder("injectExtras")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(jobTypeName, ARG_JOB)
                .returns(void.class);

        for (JobExtraClassInfo info : extrasInfo) {
            injectParamsSpecBuilder.beginControlFlow("if ($L.hasExtra($S))",
                    ARG_JOB, info.getVariableKey());

            final TypeName extraParamTypeName = TypeVariableName.get(info.getVariableType());

            injectParamsSpecBuilder.beginControlFlow("try");
            injectParamsSpecBuilder.addStatement("$L.$L = ($T) $L.findExtra($S)",
                    ARG_JOB,
                    info.getVariableName(),
                    extraParamTypeName,
                    ARG_JOB,
                    info.getVariableKey());
            injectParamsSpecBuilder.endControlFlow();
            injectParamsSpecBuilder.beginControlFlow("catch ($T e)", ClassCastException.class);
            injectParamsSpecBuilder.addStatement("throw new $T($S + \"\\\"\" + \n$L.findExtra($S).getClass().getName() + \"\\\"\")",
                    RuntimeException.class,
                    String.format("Failed to inject extra \"%s\" to \"%s\". Expected type \"%s\", but got ",
                            info.getVariableName(),
                            info.getSimpleJobName(),
                            extraParamTypeName),
                    ARG_JOB,
                    info.getVariableKey());
            injectParamsSpecBuilder.endControlFlow();
            injectParamsSpecBuilder.endControlFlow();

            if (!info.isOptional()) {
                injectParamsSpecBuilder.beginControlFlow("else");
                injectParamsSpecBuilder.addStatement("throw new $T($S)",
                        IllegalArgumentException.class,
                        String.format("Failed to inject extras to \"%s\". Required value \"%s\" not found in job params.",
                                info.getSimpleJobName(),
                                info.getVariableName()));
                injectParamsSpecBuilder.endControlFlow();
            }
        }

        return injectParamsSpecBuilder.build();
    }
}