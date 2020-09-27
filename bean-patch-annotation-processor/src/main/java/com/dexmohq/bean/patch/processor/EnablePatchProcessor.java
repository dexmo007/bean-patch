package com.dexmohq.bean.patch.processor;

import com.dexmohq.bean.patch.spi.EnablePatch;
import com.dexmohq.bean.patch.spi.Patch;
import com.google.auto.service.AutoService;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;
import java.util.Set;

@AutoService(Processor.class)
public class EnablePatchProcessor extends AbstractProcessor {

    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (final Element element : roundEnv.getElementsAnnotatedWith(EnablePatch.class)) {
            final TypeElement typeElement = (TypeElement) element;
            final TypeMirror PATCH_INTERFACE = processingEnv.getElementUtils().getTypeElement(Patch.class.getCanonicalName()).asType();

            final Optional<? extends TypeMirror> patchInterface = typeElement.getInterfaces().stream()
                    .filter(t -> processingEnv.getTypeUtils().isAssignable(processingEnv.getTypeUtils().erasure(t), PATCH_INTERFACE))
                    .findFirst();
            if (patchInterface.isEmpty()) {
                error(element, "Class annotated with @EnablePatch must implement Patch<T>");
                return true;
            }
            final TypeMirror patchedType = ((DeclaredType) patchInterface.get()).getTypeArguments().get(0);
            if (patchedType.getKind() != TypeKind.DECLARED) {
                error(element, "Class annotated with @EnablePatch must implement Patch<T> with concrete type variable T");
                return true;
            }
            final DeclaredType declaredPatchedType = (DeclaredType) patchedType;
            final String entityName = declaredPatchedType.asElement().getSimpleName().toString();
            try {
                final String pkg = processingEnv.getElementUtils().getPackageOf(element).getQualifiedName().toString();
                final JavaFileObject file = processingEnv.getFiler().createSourceFile(pkg+"."+entityName + "Patcher", element);

                final PrintWriter out = new PrintWriter(file.openWriter());
                out.println("package " + pkg + ";");
                out.println("public interface " + entityName + "Patcher {");
                out.println(entityName + " applyPatch(" + typeElement.getQualifiedName().toString() + " patch);");
                out.println("}");
                out.close();


            } catch (IOException e) {
                error(element, e.getMessage());
                return true;
            }
        }
        return true;
    }

    private void error(Element e, String msg, Object... args) {
        processingEnv.getMessager()
                .printMessage(Diagnostic.Kind.ERROR, String.format(msg, args), e);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(EnablePatch.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}