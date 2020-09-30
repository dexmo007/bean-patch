package com.dexmohq.bean.patch.processor;

import com.squareup.javapoet.*;

import javax.annotation.processing.Generated;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PatcherGenerator {

    private final Elements elements;

    public PatcherGenerator(Elements elements) {
        this.elements = elements;
    }

    public List<JavaFile> generate(Element origin,
                             TypeElement entityTypeElement,
                             TypeElement patchTypeElement,
                             Map<String, PropertyDescriptor> patchProperties) {
        final String pkg = elements.getPackageOf(patchTypeElement).getQualifiedName().toString();
        final var entityName = entityTypeElement.getSimpleName().toString();
        final var generatedAnnotation = AnnotationSpec.builder(Generated.class)
                .addMember("value", "$S", EnablePatchProcessor.class.getCanonicalName())
                .addMember("date", "$S", LocalDateTime.now().toString())
                .build();
        final var applyPatchMethod = MethodSpec.methodBuilder("applyPatch")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(TypeName.get(entityTypeElement.asType()), "entity")
                .addParameter(TypeName.get(patchTypeElement.asType()), "patch")
                .returns(TypeName.get(entityTypeElement.asType()))
                .addJavadoc(patchProperties.values().stream()
                        .map(p -> p.getType().toString() + " " + p.getName())
                        .collect(Collectors.joining(", "))
                )
                .build();
        final TypeSpec patcherInterface = TypeSpec.interfaceBuilder(entityName + "Patcher")
                .addOriginatingElement(origin)
                .addAnnotation(generatedAnnotation)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(applyPatchMethod)
                .build();

        List<CodeBlock> patchBlocks = new ArrayList<>();
        for (final PropertyDescriptor prop : patchProperties.values()) {
            final var block = CodeBlock.builder()
                    .beginControlFlow("if (patch.$N() != null)", prop.getGetter().getSimpleName().toString())
                    .addStatement("entity.$N(patch.$N())",
                            prop.getSetter().getSimpleName().toString(),
                            prop.getGetter().getSimpleName().toString())
                    .endControlFlow()
                    .build();
            patchBlocks.add(block);
        }

        final var implementation = TypeSpec.classBuilder(entityName + "PatcherImpl")
                .addOriginatingElement(origin)
                .addAnnotation(generatedAnnotation)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ClassName.get(pkg, patcherInterface.name))
                .addMethod(MethodSpec.methodBuilder(applyPatchMethod.name)
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameters(applyPatchMethod.parameters)
                        .returns(applyPatchMethod.returnType)
                        .addCode(CodeBlock.join(patchBlocks, "\n"))
                        .addCode("return entity;")
                        .build())
                .build();

        return List.of(
                JavaFile.builder(pkg, patcherInterface).build(),
                JavaFile.builder(pkg, implementation).build()
        );
    }

}
