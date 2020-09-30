package com.dexmohq.bean.patch.processor;

import com.google.common.base.CaseFormat;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {

    private final Types types;

    public Utils(Types types) {
        this.types = types;
    }


    public Map<String, PropertyDescriptor> getProperties(TypeElement element) {
        final var properties = new HashMap<String, PropertyDescriptor>();
        for (final Element child : element.getEnclosedElements()) {
            if (child.getKind() == ElementKind.FIELD
                    && !child.getModifiers().contains(Modifier.STATIC)) {
                final String fieldName = child.getSimpleName().toString();
                final TypeMirror fieldType = child.asType();
                final var property = properties.computeIfAbsent(fieldName, name -> new PropertyDescriptor(name, fieldType));
                if (!property.getType().equals(fieldType)) {
                    throw new IllegalStateException("Field type does not match property type");
                }
                property.setField(((VariableElement) child));
            } else if (child.getKind() == ElementKind.METHOD
                    && child.getModifiers().contains(Modifier.PUBLIC)
                    && !child.getModifiers().contains(Modifier.STATIC)

            ) {
                final ExecutableElement methodElement = (ExecutableElement) child;
                final int isGetter = isGetter(methodElement);
                if (isGetter > 0) {
                    final String propertyName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, methodElement.getSimpleName().toString().substring(isGetter));
                    final TypeMirror propertyType = methodElement.getReturnType();
                    final var property = properties.computeIfAbsent(propertyName, name -> new PropertyDescriptor(name, propertyType));
                    if (!types.isSameType(property.getType(), propertyType)) {
                        throw new IllegalStateException(String.format("Getter type %s does not match property type %s", propertyType, property.getType()));
                    }
                    property.setGetter(methodElement);
                }
                if (isSetter(methodElement)) {
                    final String propertyName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, methodElement.getSimpleName().toString().substring(3));
                    final TypeMirror propertyType = methodElement.getParameters().get(0).asType();
                    final var property = properties.computeIfAbsent(propertyName, name -> new PropertyDescriptor(name, propertyType));
                    if (!types.isSameType(property.getType(), propertyType)) {
                        throw new IllegalStateException("Setter type does not match property type");
                    }
                    property.setSetter(methodElement);
                }
            }
        }
        return properties;
    }

    private int isGetter(ExecutableElement element) {
        final String name = element.getSimpleName().toString();
        if (!element.getParameters().isEmpty()) {
            return 0;
        }
        if (name.startsWith("get")) {
            return 3;
        }
        if (element.getReturnType().equals(types.getPrimitiveType(TypeKind.BOOLEAN)) && name.startsWith("is")) {
            return 2;
        }
        return 0;
    }

    private boolean isSetter(ExecutableElement element) {
        return element.getReturnType().equals(types.getNoType(TypeKind.VOID))
                && element.getParameters().size() == 1
                && element.getSimpleName().toString().startsWith("set");
    }

}
