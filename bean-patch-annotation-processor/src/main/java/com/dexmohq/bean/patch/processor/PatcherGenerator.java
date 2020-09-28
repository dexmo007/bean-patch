package com.dexmohq.bean.patch.processor;

import com.squareup.javapoet.*;

import javax.annotation.processing.Generated;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

public class PatcherGenerator {

    private final Elements elements;

    public PatcherGenerator(Elements elements) {
        this.elements = elements;
    }

    public JavaFile generate(Element origin,
                             TypeElement entityTypeElement,
                             TypeElement patchTypeElement,
                             Map<String, PropertyDescriptor> patchProperties) {
        final String pkg = elements.getPackageOf(patchTypeElement).getQualifiedName().toString();
        final var entityName = entityTypeElement.getSimpleName().toString();
        final TypeSpec patcherInterface = TypeSpec.interfaceBuilder(entityName + "Patcher")
                .addOriginatingElement(origin)
                .addAnnotation(AnnotationSpec.builder(Generated.class)
                        .addMember("value", "$S", EnablePatchProcessor.class.getCanonicalName())
                        .addMember("date", "$S", LocalDateTime.now().toString())
                        .build())
                .addModifiers(Modifier.PUBLIC)
                .addMethod(
                        MethodSpec.methodBuilder("applyPatch")
                                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                .addParameter(TypeName.get(entityTypeElement.asType()), "entity")
                                .addParameter(TypeName.get(patchTypeElement.asType()), "patch")
                                .returns(TypeName.get(entityTypeElement.asType()))
                                .addJavadoc(patchProperties.values().stream()
                                        .map(p -> p.getType().toString() + " " + p.getName())
                                        .collect(Collectors.joining(", "))
                                )
                                .build()
                )
                .build();
        return JavaFile.builder(pkg, patcherInterface).build();
    }

}
