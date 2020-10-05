package com.dexmohq.bean.patch.processor;

import com.dexmohq.bean.patch.spi.Patcher;
import com.squareup.javapoet.*;
import org.springframework.stereotype.Component;

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
                    final String varName = variableNameGenerator.nextName(def.getEntityProperty());
                    code.addStatement("$T $N = entity.$N()",
                            def.getEntityReadMethod().getReturnType(),
                            varName,
                            def.getEntityReadMethod().getSimpleName().toString());
                    code.add(CodeBlock.builder()
                            .beginControlFlow("if ($N == null)", varName)
                            .addStatement("$N = new $T<>()", varName, ArrayList.class)// TODO choose correct Collection
                            .addStatement("entity.$N($N)", entityWriteMethod.getSimpleName().toString(), varName)
                            .endControlFlow()
                            .build());
                    code.addStatement("entity.$N().addAll(patch.$N())",
                            def.getEntityReadMethod().getSimpleName().toString(),
                            def.getPatchReadMethod().getSimpleName().toString()
                    );
                    break;
                case REMOVE:
                default:
                    throw new UnsupportedOperationException();
            }

            patchBlocks.add(code.endControlFlow().build());
        }
        return patchBlocks;
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

    public TypeSpec.Builder generatePatcherImplementation(TypeElement origin, List<PatchMethod> patchMethods, Map<PatchTypePair, MethodSpec> impls) {
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
                    case COLLECTION:
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
        return patcherImplSpecBuilder;
    }

    public void postProcess(TypeSpec.Builder builder, Patcher annotation) {
        switch (annotation.componentModel()) {
            case SPRING:
                builder.addAnnotation(Component.class);
                break;
            case SERVICE_LOADER:
            default:
        }
    }

    public JavaFile createFile(Element origin, TypeSpec typeSpec) {
        final String pkg = elements.getPackageOf(origin).getQualifiedName().toString();
        return JavaFile.builder(pkg, typeSpec)
                .skipJavaLangImports(true)
                .build();
    }


}
