package com.dexmohq.bean.patch.processor;

import com.squareup.javapoet.*;

import javax.annotation.processing.Generated;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PatcherGenerator {

    private final Elements elements;
    private final Utils utils;

    public PatcherGenerator(Elements elements, Utils utils) {
        this.elements = elements;
        this.utils = utils;
    }

    public AnnotationSpec generateGeneratedAnnotation() {
        return AnnotationSpec.builder(Generated.class)
                .addMember("value", "$S", EnablePatchProcessor.class.getCanonicalName())
                .addMember("date", "$S", LocalDateTime.now().toString())
                .build();
    }

    public List<JavaFile> generate(Element origin,
                                   TypeElement entityTypeElement,
                                   TypeElement patchTypeElement,
                                   Map<String, PropertyDescriptor> patchProperties) {
        final String pkg = elements.getPackageOf(patchTypeElement).getQualifiedName().toString();
        final var entityName = entityTypeElement.getSimpleName().toString();

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

        final AnnotationSpec generatedAnnotation = generateGeneratedAnnotation();
        final TypeSpec patcherInterface = TypeSpec.interfaceBuilder(entityName + "Patcher")
                .addOriginatingElement(origin)
                .addAnnotation(generatedAnnotation)
                .addModifiers(Modifier.PUBLIC)
                .addMethod(applyPatchMethod)
                .build();

        List<CodeBlock> patchBlocks = generatePatchingBlocks(patchProperties);

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

    private List<CodeBlock> generatePatchingBlocks(Map<String, PropertyDescriptor> patchProperties) {
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
        return patchBlocks;
    }

    public MethodSpec generatePatchMethod(PatchTypePair patchTypePair, Map<String, PropertyDescriptor> patchProperties) {
        final TypeName entityTypeName = TypeName.get(patchTypePair.getEntityType());
        final TypeName patchTypeName = TypeName.get(patchTypePair.getPatchType());
        final ParameterSpec entityParam = ParameterSpec.builder(entityTypeName, "entity").build();
        final ParameterSpec patchParam = ParameterSpec.builder(patchTypeName, "patch").build();

        return MethodSpec.methodBuilder("applyPatchInternal")
                .addModifiers(Modifier.PRIVATE)
                .returns(entityTypeName)
                .addParameter(entityParam)
                .addParameter(patchParam)
                .addCode(CodeBlock.join(generatePatchingBlocks(patchProperties), "\n"))
                .addCode(CodeBlock.of("return $N;", entityParam))
                .build();
    }

    public JavaFile generatePatcherImplementation(TypeElement origin, List<PatchMethod> patchMethods, Map<PatchTypePair, MethodSpec> impls) {
        final String pkg = elements.getPackageOf(origin).getQualifiedName().toString();
        final var patcherImplSpecBuilder = TypeSpec.classBuilder(origin.getSimpleName().toString() + "Impl")
                .addAnnotation(generateGeneratedAnnotation())
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ClassName.get(origin));
        impls.values().forEach(patcherImplSpecBuilder::addMethod);
        for (final PatchMethod patchMethod : patchMethods) {
            final var methodBuilder = MethodSpec.methodBuilder(patchMethod.getElement().getSimpleName().toString())
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameters(
                            patchMethod.getElement().getParameters().stream()
                                    .map(p -> ParameterSpec.builder(TypeName.get(p.asType()), p.getSimpleName().toString()).build())
                                    .collect(Collectors.toList())
                    )
                    .returns(TypeName.get(patchMethod.getElement().getReturnType()));
            for (final PatchParameter patchParam : patchMethod.getPatchParameters().values()) {
                final PatchTypePair pair = new PatchTypePair(patchMethod.getEntityType(), ((DeclaredType) patchParam.getPatchType()));
                final MethodSpec methodToCall = impls.entrySet().stream()
                        .filter(p -> utils.areEqual(p.getKey(), pair))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(String.format("Implemented method not found for %s and %s", pair.getEntityType(), pair.getPatchType())));

                switch (patchParam.getType()) {
                    case SINGLE:
                        methodBuilder.addStatement("$N($N, $N)",
                                methodToCall.name,
                                patchMethod.getEntityParameter().getSimpleName().toString(), patchParam.getParameter().getSimpleName().toString());
                        break;
                    case ARRAY:
                    case COLLECTION:
                        methodBuilder.addCode(CodeBlock.builder()
                                .beginControlFlow("for ($T e : $N)",patchParam.getPatchType(), patchParam.getParameter().getSimpleName().toString())
                                .addStatement("$N($N, $N)", methodToCall.name, patchMethod.getEntityParameter().getSimpleName().toString(), "e")
                                .endControlFlow()
                                .build());

                        break;
                }

            }
            methodBuilder.addStatement("return $N", patchMethod.getEntityParameter().getSimpleName().toString());
            patcherImplSpecBuilder.addMethod(methodBuilder.build());
        }
        return JavaFile.builder(pkg, patcherImplSpecBuilder.build()).skipJavaLangImports(true).build();
    }

}
