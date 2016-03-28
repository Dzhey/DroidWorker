package com.be.library.worker.annotations.compiler;

import com.google.common.base.CaseFormat;
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
        for (Map.Entry<String, Collection<JobExtraInfo>> entry : info.getClassesInfo().entrySet()) {
            generateInjector(entry.getKey(), entry.getValue());
        }
    }

    private String makeFieldNameForKey(String extraVariableName) {
        if (extraVariableName.length() > 0 && extraVariableName.toUpperCase().startsWith("M")) {
            extraVariableName = extraVariableName.substring(1, extraVariableName.length());
        }

        return "EXTRA_" + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, extraVariableName);
    }

    private void generateInjector(String qualifiedJobName, Collection<JobExtraInfo> extras) throws IOException {
        final JobExtraInfo firstEntry = Iterables.get(extras, 0);
        final String injectorClassName = firstEntry.getSimpleJobName() + SUFFIX;
        final String packageName = firstEntry.getPackageName();
        final TypeName jobTypeName = TypeVariableName.get(qualifiedJobName);

        mLogger.note(String.format("Generating extras injector for \"%s\" (%s)..",
                qualifiedJobName, injectorClassName));

        final TypeSpec.Builder classSpecBuilder = TypeSpec.classBuilder(injectorClassName)
                .addModifiers(Modifier.FINAL, Modifier.PUBLIC)
                .addMethod(makeCaptureExtrasMethodSpec(extras))
                .addMethod(makeInjectExtrasMethodSpec(jobTypeName, extras));

        for (JobExtraInfo info : extras) {
            classSpecBuilder.addField(FieldSpec.builder(String.class,
                    makeFieldNameForKey(info.getVariableName()),
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

    private MethodSpec makeCaptureExtrasMethodSpec(Collection<JobExtraInfo> extrasInfo) {
        final TypeName delegateTypeName = TypeVariableName.get(Consts.JOB_CONFIGURATOR_DELEGATE);
        final MethodSpec.Builder captureExtrasSpecBuilder = MethodSpec.methodBuilder("captureExtras")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(delegateTypeName);

        for (JobExtraInfo info : extrasInfo) {
            final TypeName varTypeName = TypeVariableName.get(info.getVariableType());
            captureExtrasSpecBuilder.addParameter(varTypeName, info.getVariableName(), Modifier.FINAL);
        }

        final String configuratorArgName = "configurator";
        final MethodSpec.Builder delegateMethodSpecBuilder = MethodSpec.methodBuilder("configure")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeVariableName.get(Consts.JOB_CONFIGURATOR), configuratorArgName)
                .returns(void.class);

        for (JobExtraInfo info : extrasInfo) {
            delegateMethodSpecBuilder.addStatement("$L.addExtra($L, $L)",
                    configuratorArgName,
                    makeFieldNameForKey(info.getVariableName()),
                    info.getVariableName());
        }

        final TypeSpec.Builder delegateSpecBuilder = TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(TypeVariableName.get(Consts.JOB_CONFIGURATOR_DELEGATE))
                .addMethod(delegateMethodSpecBuilder.build());

        captureExtrasSpecBuilder.addStatement("return $L", delegateSpecBuilder.build());


        return captureExtrasSpecBuilder.build();
    }

    private MethodSpec makeInjectExtrasMethodSpec(TypeName jobTypeName, Collection<JobExtraInfo> extrasInfo) {
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

        for (JobExtraInfo info : extrasInfo) {
            final String extraFieldName = makeFieldNameForKey(info.getVariableName());

            injectParamsSpecBuilder.beginControlFlow("if ($L.hasExtra($L))",
                    VAR_PARAMS, extraFieldName);

            final TypeName extraParamTypeName = TypeVariableName.get(info.getVariableType());

            injectParamsSpecBuilder.beginControlFlow("try");
            injectParamsSpecBuilder.addStatement("$L.$L = ($T) $L.getExtra($L)",
                    ARG_JOB,
                    info.getVariableName(),
                    extraParamTypeName,
                    VAR_PARAMS,
                    extraFieldName);
            injectParamsSpecBuilder.endControlFlow();
            injectParamsSpecBuilder.beginControlFlow("catch ($T e)", ClassCastException.class);
            injectParamsSpecBuilder.addStatement("throw new $T($S + \"\\\"\" + \n$L.getExtra($L).getClass().getName() + \"\\\"\")",
                    RuntimeException.class,
                    String.format("Failed to inject extra \"%s\" to \"%s\". Expected type \"%s\", but got ",
                            info.getVariableName(),
                            info.getSimpleJobName(),
                            extraParamTypeName),
                    VAR_PARAMS,
                    extraFieldName);
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
            TypeName jobTypeName, Collection<JobExtraInfo> extrasInfo) {

        final MethodSpec.Builder injectParamsSpecBuilder = MethodSpec.methodBuilder("injectExtras")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(jobTypeName, ARG_JOB)
                .returns(void.class);

        for (JobExtraInfo info : extrasInfo) {
            final String extraFieldName = makeFieldNameForKey(info.getVariableName());

            injectParamsSpecBuilder.beginControlFlow("if ($L.hasExtra($L))",
                    ARG_JOB, extraFieldName);

            final TypeName extraParamTypeName = TypeVariableName.get(info.getVariableType());

            injectParamsSpecBuilder.beginControlFlow("try");
            injectParamsSpecBuilder.addStatement("$L.$L = ($T) $L.findExtra($L)",
                    ARG_JOB,
                    info.getVariableName(),
                    extraParamTypeName,
                    ARG_JOB,
                    extraFieldName);
            injectParamsSpecBuilder.endControlFlow();
            injectParamsSpecBuilder.beginControlFlow("catch ($T e)", ClassCastException.class);
            injectParamsSpecBuilder.addStatement("throw new $T($S + \"\\\"\" + \n$L.findExtra($L).getClass().getName() + \"\\\"\")",
                    RuntimeException.class,
                    String.format("Failed to inject extra \"%s\" to \"%s\". Expected type \"%s\", but got ",
                            info.getVariableName(),
                            info.getSimpleJobName(),
                            extraParamTypeName),
                    ARG_JOB,
                    extraFieldName);
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
