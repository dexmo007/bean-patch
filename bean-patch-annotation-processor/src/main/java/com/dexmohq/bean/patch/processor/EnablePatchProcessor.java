package com.dexmohq.bean.patch.processor;

import com.dexmohq.bean.patch.spi.Patch;
import com.dexmohq.bean.patch.spi.Patcher;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

@AutoService(Processor.class)
public class EnablePatchProcessor extends BaseProcessor<TypeElement> {

    private Utils utils;
    private PatcherGenerator patcherGenerator;
    private TypeElement patchInterface;

    public EnablePatchProcessor() {
        super(Patcher.class, TypeElement.class);
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        utils = new Utils(types, elements);
        patcherGenerator = new PatcherGenerator(elements, utils);
        patchInterface = processingEnv.getElementUtils().getTypeElement(Patch.class.getCanonicalName());
    }

    private PatchMethod extractPatchMethod(ExecutableElement method) {
        if (!method.getTypeParameters().isEmpty()) {
            throw new ProcessingException(method, "Patch method cannot have type parameters");
        }
        final TypeMirror entityType = method.getReturnType();
        if (entityType.getKind() != TypeKind.DECLARED) {
            throw new ProcessingException(method, "Entity type must be a declared type");
        }
        VariableElement entityParam = null;
        Integer entityParamIndex = null;
        final Map<Integer, PatchParameter> patchParameters = new HashMap<>();
        final Set<DeclaredType> patchTypes = utils.typeMirrorSet();
        for (int i = 0; i < method.getParameters().size(); i++) {
            final VariableElement parameter = method.getParameters().get(i);
            if (types.isSameType(parameter.asType(), entityType)) {
                entityParam = parameter;
                entityParamIndex = i;
            } else if (utils.implementsInterfaceConcretely(parameter.asType(), patchInterface, entityType)) {
                patchParameters.put(i, new PatchParameter(parameter, PatchParameterType.SINGLE, parameter.asType()));
                patchTypes.add((DeclaredType) parameter.asType());
            } else if (parameter.asType().getKind() == TypeKind.ARRAY) {
                final TypeMirror componentType = ((ArrayType) parameter.asType()).getComponentType();
                if (componentType.getKind() != TypeKind.DECLARED) {
                    throw new ProcessingException(parameter, "Component type of patch parameter must be a declared type");
                }
                if (!utils.implementsInterfaceConcretely(componentType, patchInterface, entityType)) {
                    throw new ProcessingException(parameter, "Component type must implement patch of entity");
                }
                patchParameters.put(i, new PatchParameter(parameter, PatchParameterType.ARRAY, componentType));
                patchTypes.add(((DeclaredType) componentType));
            } else if (utils.implementsInterface(parameter.asType(), elements.getTypeElement(Collection.class.getCanonicalName()))) {
                final TypeMirror elementType = utils.findElementTypeOfCollection(parameter.asType());
                if (elementType.getKind() != TypeKind.DECLARED) {
                    throw new ProcessingException(parameter, "Element type of patch parameter must be a declared type");
                }
                if (!utils.implementsInterfaceConcretely(elementType, patchInterface, entityType)) {
                    throw new ProcessingException(parameter, "Element type must implement patch of entity");
                }
                patchParameters.put(i, new PatchParameter(parameter, PatchParameterType.COLLECTION, elementType));
                patchTypes.add(((DeclaredType) elementType));
            } else {
                throw new ProcessingException(parameter, "Patch method parameter must either be of entity type or patch type");
            }
        }
        if (entityParam == null) {
            throw new ProcessingException(method, "Patch method must have a parameter of the entity type");
        }
        if (patchParameters.isEmpty()) {
            throw new ProcessingException(method, "Patch method must include parameter(s) of the patch type");
        }


        return PatchMethod.builder()
                .element(method)
                .entityParameter(entityParam)
                .entityParameterIndex(entityParamIndex)
                .entityType((DeclaredType) entityType)
                .patchParameters(patchParameters)
                .patchTypes(patchTypes)
                .build();
    }

    @Override
    protected void process(TypeElement element) throws IOException {

        if (element.getKind() != ElementKind.INTERFACE) {
            error(element, "Class annotated with @Patcher must be an interface");
            return;
        }
        final List<PatchMethod> patchMethods = new ArrayList<>();
        for (final Element child : element.getEnclosedElements()) {
            if (child.getKind() == ElementKind.METHOD) {
                final ExecutableElement method = (ExecutableElement) child;
                if (method.isDefault()) {
                    continue;
                }
                final PatchMethod patchMethod = extractPatchMethod(method);
                patchMethods.add(patchMethod);
            }
        }
        final Set<PatchTypePair> patchTypePairs = patchMethods.stream()
                .flatMap(patchMethod -> patchMethod.getPatchTypes().stream()
                        .map(patchType -> new PatchTypePair(patchMethod.getEntityType(), patchType)))
                .collect(utils.toTypeMirrorLikeSet());

        Map<PatchTypePair, MethodSpec> patchMethodImpls = new HashMap<>();
        for (final PatchTypePair patchTypePair : patchTypePairs) {
            final var patchProperties = utils.getProperties(patchTypePair.getPatchTypeElement());
            final MethodSpec patchMethodSpec = patcherGenerator.generatePatchMethod(patchTypePair, patchProperties);
            patchMethodImpls.put(patchTypePair, patchMethodSpec);
        }
        final JavaFile javaFile = patcherGenerator.generatePatcherImplementation(element, patchMethods, patchMethodImpls);
        javaFile.writeTo(filer);
        generateServiceFile(element, javaFile);
    }

    private void generateServiceFile(TypeElement patcherInterface, JavaFile impl) throws IOException {
        final String fqImpl = impl.packageName.isEmpty()
                ? impl.typeSpec.name
                : impl.packageName + "." + impl.typeSpec.name;
        final String resourceFile = "META-INF/services/" + patcherInterface.getQualifiedName().toString();
        final FileObject file = filer.createResource(StandardLocation.CLASS_OUTPUT, "", resourceFile, patcherInterface);
        final BufferedWriter writer = new BufferedWriter(file.openWriter());
        writer.write(fqImpl);
        writer.newLine();
        writer.flush();
        writer.close();
    }
}
