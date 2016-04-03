package com.be.library.worker.annotations.compiler;

import com.be.library.worker.annotations.Inherited;
import com.be.library.worker.annotations.JobExtra;
import com.be.library.worker.annotations.JobFlag;
import com.google.auto.service.AutoService;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

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
import javax.lang.model.util.ElementFilter;

@AutoService(Processor.class)
public class JobProcessor extends AbstractProcessor {

    public static final String EXTRA_ANNOTATION_PRINTABLE = "@" + JobExtra.class.getSimpleName();
    public static final String FLAG_ANNOTATION_PRINTABLE = "@" + JobFlag.class.getSimpleName();
    public static final String INHERITED_ANNOTATION_PRINTABLE = "@" + Inherited.class.getSimpleName();
    public static final String PROCESSOR_NAME = "@" + JobProcessor.class.getSimpleName();

    private ErrorReporter mErrorReporter;
    private Logger mLogger;
    private ProcessingEnvironment mProcessingEnvironment;
    private List<FieldInfo> mDeferredFields;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        mProcessingEnvironment = processingEnv;
        mErrorReporter = new ErrorReporter(processingEnv);
        mLogger = new Logger(processingEnv);
        mDeferredFields = Lists.newArrayList();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return ImmutableSet.of(JobExtra.class.getName(), JobFlag.class.getName(), Inherited.class.getName());
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
            for (FieldInfo fieldInfo : mDeferredFields) {
                mErrorReporter.reportError(String.format("Did not generate injector for \"%s.%s\""
                        + " because it references undefined types",
                        fieldInfo.getQualifiedJobName(),
                        fieldInfo.getVariableSimpleName()));
            }

            return false;
        }

        final JobClassInfo jobClassInfo = new JobClassInfo(mProcessingEnvironment);
        Collection<? extends Element> extraElements =
                roundEnv.getElementsAnnotatedWith(JobExtra.class);
        Collection<? extends Element> flagElements =
                roundEnv.getElementsAnnotatedWith(JobFlag.class);
        if (!mDeferredFields.isEmpty()) {
            extraElements = Lists.newArrayList(Iterables.concat(extraElements, getDeferredExtras()));
            flagElements = Lists.newArrayList(Iterables.concat(flagElements, getDeferredFlags()));
            mDeferredFields.clear();
        }

        try {
            processExtraElements(jobClassInfo, extraElements);
            processFlagElements(jobClassInfo, flagElements);
            processInheritedElements(jobClassInfo, roundEnv.getElementsAnnotatedWith(Inherited.class));

            final JobExtraInjectorGenerator generator =
                    new JobExtraInjectorGenerator(mProcessingEnvironment);
            generator.generateCode(jobClassInfo);

        } catch (AbortProcessingException e) {
            // proceed

        } catch (Exception e) {
            String trace = Throwables.getStackTraceAsString(e);
            mErrorReporter.reportError(PROCESSOR_NAME + " threw an exception: " + trace);
        }
        mLogger.note(String.format("worker compiler finished in %dms", System.currentTimeMillis() - startTime));

        return false;
    }

    private void processInheritedElements(JobClassInfo classInfo, Collection<? extends Element> elements) {
        for (Element element : elements) {
            try {
                if (element.getKind() == ElementKind.CLASS) {
                    final TypeElement typeElement = TypeSimplifier.toTypeElement(element.asType());
                    final List<VariableElement> fields = ElementFilter.fieldsIn(typeElement.getEnclosedElements());
                    boolean hasInheritedField = false;
                    for (VariableElement field : fields) {
                        if (field.getAnnotation(Inherited.class) != null) {
                            hasInheritedField = true;
                            break;
                        }
                    }

                    if (!hasInheritedField) {
                        mErrorReporter.reportError(String.format("inherited \"%s\" class has no @%s-annotated fields",
                                typeElement.getQualifiedName().toString(),
                                Inherited.class.getSimpleName()));
                    }
                    continue;
                }

                classInfo.registerJobExtraInfo(processInheritedElement(element));

            } catch (AbortProcessingException e) {
                // We abandoned this type; continue with the next.

            } catch (Exception e) {
                String trace = Throwables.getStackTraceAsString(e);
                mErrorReporter.reportError(PROCESSOR_NAME + "processor threw an exception: " + trace, element);
            }
        }
    }

    private void processFlagElements(JobClassInfo classInfo, Collection<? extends Element> elements) {
        for (Element element : elements) {
            try {
                classInfo.registerJobExtraInfo(processFlagElement(element));

            } catch (AbortProcessingException e) {
                // We abandoned this type; continue with the next.

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

        final JobExtraInfo info = new JobExtraInfo(variableElement, mProcessingEnvironment);

        info.init();

        return info;
    }

    private inheritableFieldInfo processInheritedElement(Element element) {
        final Inherited inherited = element.getAnnotation(Inherited.class);
        if (inherited == null) {
            mErrorReporter.abortWithError("annotation processor for " +
                    INHERITED_ANNOTATION_PRINTABLE +
                    " was invoked with a type"+
                    " that does not have that annotation; this is probably a compiler bug", element);
        }

        if (element.getKind() != ElementKind.FIELD) {
            mErrorReporter.abortWithError(FLAG_ANNOTATION_PRINTABLE +
                    " only applies to fields and classes", element);
        }

        final VariableElement variableElement = (VariableElement) element;

        final inheritableFieldInfo info = new inheritableFieldInfo(variableElement, mProcessingEnvironment);
        info.init();

        return info;
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

        final JobFlagInfo info = new JobFlagInfo(variableElement, mProcessingEnvironment);
        info.init();

        return info;
    }

    private Iterable<Element> getDeferredExtras() {
        return Iterables.transform(Iterables.filter(mDeferredFields, new Predicate<FieldInfo>() {
            @Override
            public boolean apply(FieldInfo input) {
                return input.getFieldAnnotationType().equals(JobExtra.class);
            }
        }), new Function<FieldInfo, Element>() {
            @Override
            public Element apply(FieldInfo input) {
                return input.getElement();
            }
        });
    }

    private Iterable<Element> getDeferredFlags() {
        return Iterables.transform(Iterables.filter(mDeferredFields, new Predicate<FieldInfo>() {
            @Override
            public boolean apply(FieldInfo input) {
                return input.getFieldAnnotationType().equals(JobExtra.class);
            }
        }), new Function<FieldInfo, Element>() {
            @Override
            public Element apply(FieldInfo input) {
                return input.getElement();
            }
        });
    }
}
