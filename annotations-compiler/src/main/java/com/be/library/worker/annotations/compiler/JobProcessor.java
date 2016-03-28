package com.be.library.worker.annotations.compiler;

import com.be.library.worker.annotations.JobExtra;
import com.be.library.worker.annotations.JobFlag;
import com.google.auto.service.AutoService;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;

import java.util.Collection;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

@AutoService(Processor.class)
public class JobProcessor extends AbstractProcessor {

    public static final String EXTRA_ANNOTATION_PRINTABLE = "@" + JobExtra.class.getSimpleName();
    public static final String FLAG_ANNOTATION_PRINTABLE = "@" + JobFlag.class.getSimpleName();
    public static final String PROCESSOR_NAME = "@" + JobProcessor.class.getSimpleName();

    private ErrorReporter mErrorReporter;
    private Logger mLogger;
    private ProcessingEnvironment mProcessingEnvironment;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        mProcessingEnvironment = processingEnv;
        mErrorReporter = new ErrorReporter(processingEnv);
        mLogger = new Logger(processingEnv);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return ImmutableSet.of(JobExtra.class.getName(), JobFlag.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        final long startTime = System.currentTimeMillis();
        mLogger.note(String.format("worker compiler started processing on %d elements..", annotations.size()));

        final JobClassInfo jobClassInfo = new JobClassInfo();

        try {
            processExtraElements(jobClassInfo, roundEnv);

            final JobExtraInjectorGenerator generator =
                    new JobExtraInjectorGenerator(mProcessingEnvironment);
            generator.generateCode(jobClassInfo);

        } catch (Exception e) {
            String trace = Throwables.getStackTraceAsString(e);
            mErrorReporter.reportError(PROCESSOR_NAME + "processor threw an exception: " + trace);
        }
        mLogger.note(String.format("worker compiler finished in %dms", System.currentTimeMillis() - startTime));

        return true;
    }

    private void processFlagElements(JobClassInfo classInfo, RoundEnvironment roundEnv) {
        final Collection<? extends Element> annotatedElements =
                roundEnv.getElementsAnnotatedWith(JobFlag.class);

        for (Element element : annotatedElements) {
            try {
                classInfo.registerJobExtraInfo(processExtraElement(element));

            } catch (AbortProcessingException e) {
                // We abandoned this type; continue with the next.

            } catch (Exception e) {
                String trace = Throwables.getStackTraceAsString(e);
                mErrorReporter.reportError(PROCESSOR_NAME + "processor threw an exception: " + trace, element);
            }
        }
    }

    private void processExtraElements(JobClassInfo classInfo, RoundEnvironment roundEnv) {
        final Collection<? extends Element> annotatedElements =
                roundEnv.getElementsAnnotatedWith(JobExtra.class);

        for (Element element : annotatedElements) {
            try {
                classInfo.registerJobExtraInfo(processExtraElement(element));

            } catch (AbortProcessingException e) {
                // We abandoned this type; continue with the next.

            } catch (Exception e) {
                String trace = Throwables.getStackTraceAsString(e);
                mErrorReporter.reportError(PROCESSOR_NAME + "processor threw an exception: " + trace, element);
            }
        }
    }

    private JobExtraInfo processExtraElement(Element element) {
        final JobExtra jobExtra = element.getAnnotation(JobExtra.class);
        if (jobExtra == null) {
            mErrorReporter.abortWithError("annotation processor for " +
                    EXTRA_ANNOTATION_PRINTABLE +
                    " was invoked with a type"+
                    " that does not have that annotation; this is probably a compiler bug", element);
        }

        if (element.getKind() != ElementKind.FIELD) {
            mErrorReporter.abortWithError(EXTRA_ANNOTATION_PRINTABLE + " only applies to classes", element);
        }

        final VariableElement variableElement = (VariableElement) element;

        return new JobExtraInfo(variableElement, mProcessingEnvironment);
    }

    private JobExtraInfo processFlagElement(Element element) {
        final JobFlag jobFlag = element.getAnnotation(JobFlag.class);
        if (jobFlag == null) {
            mErrorReporter.abortWithError("annotation processor for " +
                    FLAG_ANNOTATION_PRINTABLE +
                    " was invoked with a type"+
                    " that does not have that annotation; this is probably a compiler bug", element);
        }

        if (element.getKind() != ElementKind.FIELD) {
            mErrorReporter.abortWithError(FLAG_ANNOTATION_PRINTABLE + " only applies to classes", element);
        }

        final VariableElement variableElement = (VariableElement) element;

        return new JobExtraInfo(variableElement, mProcessingEnvironment);
    }
}
