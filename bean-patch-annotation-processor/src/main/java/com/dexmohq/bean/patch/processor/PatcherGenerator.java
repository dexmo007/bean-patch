package com.dexmohq.bean.patch.processor;

import com.dexmohq.annotation.processing.Utils;
import com.dexmohq.bean.patch.processor.model.*;
import com.squareup.javapoet.*;

import javax.annotation.processing.Generated;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class PatcherGenerator {

    private final ProcessingEnvironment processingEnv;
    private final Elements elements;
    private final Types types;
    private final Utils utils;

    public PatcherGenerator(ProcessingEnvironment processingEnv, Elements elements, Types types, Utils utils) {
        this.processingEnv = processingEnv;
        this.elements = elements;
        this.types = types;
        this.utils = utils;
    }

    public AnnotationSpec generateGeneratedAnnotation() {
        return AnnotationSpec.builder(Generated.class)
                .addMember("value", "$S", EnablePatchProcessor.class.getCanonicalName())
                .addMember("date", "$S", LocalDateTime.now().toString())
                .build();
    }

    private List<CodeBlock> generatePatchingBlocks(List<PatchPropertyDefinition> patchPropertyDefinitions) {
        final List<CodeBlock> patchBlocks = new ArrayList<>();
        final VariableNameGenerator variableNameGenerator = new VariableNameGenerator("entity", "patch");
        for (final PatchPropertyDefinition def : patchPropertyDefinitions) {
            final var patchReadMethod = def.getPatchReadMethod();
            final var entityWriteMethod = def.getEntityWriteMethod();
            final var code = CodeBlock.builder()
                    .beginControlFlow("if (patch.$N() != null)", patchReadMethod.getSimpleName().toString());
            switch (def.getPatchType()) {
                case SET:
                    code.addStatement("entity.$N(patch.$N())",
                            entityWriteMethod.getSimpleName().toString(),
                            patchReadMethod.getSimpleName().toString());
                    break;
                case ADD:
                    genAdd(variableNameGenerator, def, code);
                    break;
                case REMOVE:
                    genRemove(variableNameGenerator, def, code);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }

            patchBlocks.add(code.endControlFlow().build());
        }
        return patchBlocks;
    }

    private void genAdd(VariableNameGenerator variableNameGenerator, PatchPropertyDefinition def, CodeBlock.Builder code) {
        final String varName = variableNameGenerator.nextName(def.getEntityProperty());
        code.addStatement("$T $N = entity.$N()",
                def.getEntityReadMethod().getReturnType(),
                varName,
                def.getEntityReadMethod().getSimpleName().toString());
        final TypeMirror entityType = def.getEntityProperty().getType();
        code.add(CodeBlock.builder()
                .beginControlFlow("if ($N == null)", varName)
                .addStatement("$N = new $T<>()", varName,
                        types.erasure(chooseCollectionType(entityType)))
                .addStatement("entity.$N($N)", def.getEntityWriteMethod().getSimpleName().toString(), varName)
                .endControlFlow()
                .build());
        if (def.getSourceCardinality() == Cardinality.ONE) {
            // add single patch value
            code.addStatement("entity.$N().add(patch.$N())",
                    def.getEntityReadMethod().getSimpleName().toString(),
                    def.getPatchReadMethod().getSimpleName().toString()
            );
        } else {
            code.addStatement("entity.$N().addAll(patch.$N())",
                    def.getEntityReadMethod().getSimpleName().toString(),
                    def.getPatchReadMethod().getSimpleName().toString()
            );
        }
    }

    private void genRemove(VariableNameGenerator variableNameGenerator, PatchPropertyDefinition def, CodeBlock.Builder code) {
        final String varName = variableNameGenerator.nextName(def.getEntityProperty());
        code.addStatement("$T $N = entity.$N()",
                def.getEntityReadMethod().getReturnType(),
                varName,
                def.getEntityReadMethod().getSimpleName().toString());
        final String method;
        switch (def.getSourceCardinality()) {
            case ONE:
                method = "remove";
                break;
            case MANY:
                method = "removeAll";
                break;
            default:
                throw new IllegalStateException();

        }
        code.add(CodeBlock.builder()
                .beginControlFlow("if ($N != null)", varName)
                .addStatement("$N.$N(patch.$N())", varName, method, def.getPatchReadMethod().getSimpleName().toString())
                .endControlFlow()
                .build());
    }

    private DeclaredType chooseCollectionType(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED || !utils.implementsInterface(type, Collection.class)) {
            throw new IllegalArgumentException();
        }
        final TypeElement typeElement = ((TypeElement) ((DeclaredType) type).asElement());
        if (typeElement.getKind() == ElementKind.CLASS && !typeElement.getModifiers().contains(Modifier.ABSTRACT)) {
            if (!utils.hasPublicNoArgsConstructor(typeElement)) {
                throw new IllegalStateException(String.format("Collection of type %s has no default constructor", type));
            }
            return (DeclaredType) type;
        }
        return ((DeclaredType) elements.getTypeElement(chooseCollectionClass(type).getCanonicalName()).asType());
    }

    @SuppressWarnings("rawtypes")
    private Class<? extends Collection> chooseCollectionClass(TypeMirror type) {
        if (utils.implementsInterface(type, List.class)) {
            return ArrayList.class;
        }
        if (utils.implementsInterface(type, SortedSet.class)) {
            return TreeSet.class;
        }
        if (utils.implementsInterface(type, Set.class)) {
            return HashSet.class;
        }
        return ArrayList.class;
    }

    public MethodSpec generatePatchMethod(PatchDefinition patchDefinition) {
        final TypeName entityTypeName = TypeName.get(patchDefinition.getEntityType());
        final TypeName patchTypeName = TypeName.get(patchDefinition.getPatchType());
        final ParameterSpec entityParam = ParameterSpec.builder(entityTypeName, "entity").build();
        final ParameterSpec patchParam = ParameterSpec.builder(patchTypeName, "patch").build();

        return MethodSpec.methodBuilder("applyPatchInternal")
                .addModifiers(Modifier.PRIVATE)
                .returns(entityTypeName)
                .addParameter(entityParam)
                .addParameter(patchParam)
                .addCode(CodeBlock.join(generatePatchingBlocks(patchDefinition.getPropertyDefinitions()), "\n"))
                .addCode(CodeBlock.of("return $N;", entityParam))
                .build();
    }

    // TODO ensure applyPatchInternal is free to use
    public TypeSpec generatePatcherImplementation(PatcherDefinition patcherDefinition, Map<PatchTypePair, MethodSpec> impls) {
        final var origin = patcherDefinition.getOrigin();
        final var patcherImplSpecBuilder = TypeSpec.classBuilder(origin.getSimpleName().toString() + "Impl")
                .addAnnotation(generateGeneratedAnnotation())
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ClassName.get(origin));
        impls.values().forEach(patcherImplSpecBuilder::addMethod);
        for (final PatchMethod patchMethod : patcherDefinition.getPatchMethods()) {
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
                final MethodSpec methodToCall = impls.get(pair);
                if (methodToCall == null) {
                    throw new IllegalStateException(String.format("Implemented method not found for %s and %s", pair.getEntityType(), pair.getPatchType()));
                }

                switch (patchParam.getType()) {
                    case SINGLE:
                        methodBuilder.addStatement("$N($N, $N)",
                                methodToCall.name,
                                patchMethod.getEntityParameter().getSimpleName().toString(), patchParam.getParameter().getSimpleName().toString());
                        break;
                    case ARRAY:
                    case ITERABLE:
                        methodBuilder.addCode(CodeBlock.builder()
                                .beginControlFlow("for ($T e : $N)", patchParam.getPatchType(), patchParam.getParameter().getSimpleName().toString())
                                .addStatement("$N($N, $N)", methodToCall.name, patchMethod.getEntityParameter().getSimpleName().toString(), "e")
                                .endControlFlow()
                                .build());

                        break;
                }

            }
            methodBuilder.addStatement("return $N", patchMethod.getEntityParameter().getSimpleName().toString());
            patcherImplSpecBuilder.addMethod(methodBuilder.build());
        }
        patcherDefinition.getComponentModel()
                .postProcessType(patcherImplSpecBuilder, processingEnv);
        return patcherImplSpecBuilder.build();
    }

    public JavaFile createFile(Element origin, TypeSpec typeSpec) {
        final String pkg = elements.getPackageOf(origin).getQualifiedName().toString();
        return JavaFile.builder(pkg, typeSpec)
                .skipJavaLangImports(true)
                .build();
    }


}
