package com.be.library.worker.annotations.compiler;

import com.be.library.worker.annotations.JobExtra;
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

    public static final String ANNOTATION_PRINTABLE = "@" + JobExtra.class.getSimpleName();
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
        return ImmutableSet.of(JobExtra.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        final long startTime = System.currentTimeMillis();
        mLogger.note(String.format("worker compiler started processing on %d elements..", annotations.size()));

        final Collection<? extends Element> annotatedElements =
                roundEnv.getElementsAnnotatedWith(JobExtra.class);
        final JobClassInfo jobClassInfo = new JobClassInfo();

        for (Element element : annotatedElements) {
            try {
                jobClassInfo.registerJobExtraInfo(processElement(element));

            } catch (AbortProcessingException e) {
                // We abandoned this type; continue with the next.

            } catch (Exception e) {
                // Don't propagate this exception, which will confusingly crash the compiler.
                // Instead, report a compiler error with the stack trace.
                String trace = Throwables.getStackTraceAsString(e);
                mErrorReporter.reportError(PROCESSOR_NAME + "processor threw an exception: " + trace, element);
            }
        }

        try {
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

    private JobExtraClassInfo processElement(Element element) {
        final JobExtra jobExtra = element.getAnnotation(JobExtra.class);
        if (jobExtra == null) {
            // This shouldn't happen unless the compilation environment is buggy,
            // but it has happened in the past and can crash the compiler.
            mErrorReporter.abortWithError("annotation processor for " +
                    ANNOTATION_PRINTABLE +
                    " was invoked with a type"+
                    " that does not have that annotation; this is probably a compiler bug", element);
        }

        if (element.getKind() != ElementKind.FIELD) {
            mErrorReporter.abortWithError(ANNOTATION_PRINTABLE + " only applies to classes", element);
        }

        final VariableElement variableElement = (VariableElement) element;

        return new JobExtraClassInfo(variableElement, mProcessingEnvironment);
    }
}
