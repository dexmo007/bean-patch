package com.dexmohq.bean.patch.processor;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Set;

import static java.util.stream.Collectors.joining;

public abstract class BaseProcessor<T extends Element> extends AbstractProcessor {

    protected final Class<? extends Annotation> annotation;
    protected final Class<T> elementType;

    protected Types types;
    protected Elements elements;
    protected Messager messager;
    protected Filer filer;

    protected BaseProcessor(Class<? extends Annotation> annotation, Class<T> elementType) {
        this.annotation = annotation;
        this.elementType = elementType;
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        types = processingEnv.getTypeUtils();
        elements = processingEnv.getElementUtils();
        messager = processingEnv.getMessager();
        filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (final Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
            try {
                if (!elementType.isAssignableFrom(element.getClass())) {
                    error(element, "Element annotated with %s should be %s, but was %s", annotation.getSimpleName(), elementType.getSimpleName(), element.getClass().getSimpleName());
                    return true;
                }
                process(elementType.cast(element));
            } catch (ProcessingException e) {
                error(e.getOrigin(), e.getMessage());
            } catch (IOException e) {
                error(element, e.getMessage());
            } catch (Exception e) {
                error(element, "Unexpected error during processing of %s: %s: %s:\n%s",
                        getClass(), e.getClass(), e.getMessage(), Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).collect(joining("\n")));
            }
        }
        return true;
    }

    protected abstract void process(T element) throws Exception;

    protected void error(Element e, String msg, Object... args) {
        messager.printMessage(Diagnostic.Kind.ERROR, String.format(msg, args), e);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(annotation.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
