package com.be.library.worker.annotations.compiler;

import com.google.common.collect.Iterables;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import com.be.android.library.worker.models;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

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

    private void generateInjector(String jobName, Collection<JobExtraClassInfo> extras) throws IOException, ClassNotFoundException {
        final Types types = mProcessingEnvironment.getTypeUtils();

        final JobExtraClassInfo firstEntry = Iterables.get(extras, 0);
        final String injectorClassName = jobName + SUFFIX;
        final String packageName = firstEntry.getPackageName();

        final TypeMirror jobParamsTypeMirror = mProcessingEnvironment.getElementUtils()
                .getTypeElement(Consts.JOB_PARAMS_TYPE)
                .asType();

        final MethodSpec.Builder injectParamsSpecBuilder = MethodSpec.methodBuilder("injectParams")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(TypeName.get(jobParamsTypeMirror), "params")
                .returns(void.class);

        for (JobExtraClassInfo info : extras) {.
        }

        final TypeSpec injectorSpec = TypeSpec.classBuilder(injectorClassName)
                .addModifiers(Modifier.DEFAULT, Modifier.FINAL)
                .addMethod(injectParamsSpecBuilder.build())
                .build();

        final JavaFile javaFile = JavaFile.builder(packageName, injectorSpec)
                .build();

        javaFile.writeTo(System.out);
    }
}
