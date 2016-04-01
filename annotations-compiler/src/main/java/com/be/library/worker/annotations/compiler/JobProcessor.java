package com.be.library.worker.annotations.compiler;

import com.be.library.worker.annotations.JobExtra;
import com.be.library.worker.annotations.JobFlag;
import com.google.auto.service.AutoService;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.lang.annotation.AnnotationTypeMismatchException;
import java.util.Collection;
import java.util.List;
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
    private List<Element> mDeferredExtraElements;
    private List<Element> mDeferredFlagElements;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        mProcessingEnvironment = processingEnv;
        mErrorReporter = new ErrorReporter(processingEnv);
        mLogger = new Logger(processingEnv);
        mDeferredExtraElements = Lists.newArrayList();
        mDeferredFlagElements = Lists.newArrayList();
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

        if (roundEnv.processingOver()) {
            for (Element element : mDeferredExtraElements) {
                mErrorReporter.reportError("Did not generate injector for " + element.toString()
                        + " because it references undefined types", element);
            }
            for (Element element : mDeferredFlagElements) {
                mErrorReporter.reportError("Did not generate injector for " + element.toString()
                        + " because it references undefined types", element);
            }

            return false;
        }

        final JobClassInfo jobClassInfo = new JobClassInfo();
        Collection<? extends Element> extraElements =
                roundEnv.getElementsAnnotatedWith(JobExtra.class);
        Collection<? extends Element> flagElements =
                roundEnv.getElementsAnnotatedWith(JobFlag.class);
        if (!mDeferredExtraElements.isEmpty()) {
            extraElements = Lists.newArrayList(Iterables.concat(extraElements, mDeferredExtraElements));
            mDeferredExtraElements.clear();
        }
        if (!mDeferredFlagElements.isEmpty()) {
            flagElements = Lists.newArrayList(Iterables.concat(flagElements, mDeferredFlagElements));
            mDeferredFlagElements.clear();
        }

        try {
            processExtraElements(jobClassInfo, extraElements);
            processFlagElements(jobClassInfo, flagElements);

            final JobExtraInjectorGenerator generator =
                    new JobExtraInjectorGenerator(mProcessingEnvironment);
            generator.generateCode(jobClassInfo);

        } catch (Exception e) {
            String trace = Throwables.getStackTraceAsString(e);
            mErrorReporter.reportError(PROCESSOR_NAME + " threw an exception: " + trace);
        }
        mLogger.note(String.format("worker compiler finished in %dms", System.currentTimeMillis() - startTime));

        return false;
    }

    private void processFlagElements(JobClassInfo classInfo, Collection<? extends Element> elements) {
        for (Element element : elements) {
            try {
                classInfo.registerJobExtraInfo(processFlagElement(element));

            } catch (AbortProcessingException e) {
                // We abandoned this type; continue with the next.

            } catch (AnnotationTypeMismatchException e) {
                mDeferredFlagElements.add(element);

            } catch (Exception e) {
                String trace = Throwables.getStackTraceAsString(e);
                mErrorReporter.reportError(PROCESSOR_NAME + "processor threw an exception: " + trace, element);
            }
        }
    }

    private void processExtraElements(JobClassInfo classInfo, Collection<? extends Element> elements) {
        for (Element element : elements) {
            try {
                classInfo.registerJobExtraInfo(processExtraElement(element));

            } catch (AbortProcessingException e) {
                // We abandoned this type; continue with the next.

            } catch (AnnotationTypeMismatchException e) {
                mDeferredExtraElements.add(element);
                throw e;

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

    private JobFlagInfo processFlagElement(Element element) {
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

        return new JobFlagInfo(variableElement, mProcessingEnvironment);
    }
}
