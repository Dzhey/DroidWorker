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
        final String jobArgName = "job";
        final String paramsVarName = "params";

        final TypeName jobParamsTypeName = TypeVariableName.get(Consts.JOB_PARAMS_TYPE);

        final MethodSpec.Builder injectParamsSpecBuilder = MethodSpec.methodBuilder("injectExtras")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(jobTypeName, jobArgName)
                .returns(void.class);

        injectParamsSpecBuilder.addStatement("final $T $L = $L.getParams()",
                jobParamsTypeName, paramsVarName, jobArgName);

        for (JobExtraClassInfo info : extrasInfo) {
            injectParamsSpecBuilder.beginControlFlow("if ($L.hasExtra($S))",
                    paramsVarName, info.getVariableKey());

            final TypeName extraParamTypeName = TypeVariableName.get(info.getVariableType());

            injectParamsSpecBuilder.addStatement("$L.$L = ($T) $L.getExtra($S)",
                    jobArgName,
                    info.getVariableName(),
                    extraParamTypeName,
                    paramsVarName,
                    info.getVariableKey());
            injectParamsSpecBuilder.endControlFlow();
        }

        return injectParamsSpecBuilder.build();
    }
}
