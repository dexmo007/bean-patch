package com.dexmohq.bean.patch.processor.beans;

import com.google.common.base.CaseFormat;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.HashMap;
import java.util.Map;

import static javax.lang.model.element.ElementKind.FIELD;
import static javax.lang.model.element.ElementKind.METHOD;
import static javax.lang.model.element.Modifier.*;

public class PropertiesIntrospector {

    private final Types types;
    private final Map<TypeElement, Map<String, PropertyDescriptor>> cache;

    public PropertiesIntrospector(Types types) {
        this.types = types;
        cache = new HashMap<>();
    }

    public Map<String, PropertyDescriptor> getProperties(TypeElement element) {
        return cache.computeIfAbsent(element, this::doGetProperties);
    }

    private Map<String, PropertyDescriptor> doGetProperties(TypeElement element) {
        final var properties = new HashMap<String, PropertyDescriptor>();
        for (final Element child : element.getEnclosedElements()) {
            if (child.getModifiers().contains(STATIC)) {
                continue;
            }
            if (child.getKind() == FIELD) {
                final String fieldName = child.getSimpleName().toString();
                final TypeMirror fieldType = child.asType();
                final var property = properties.computeIfAbsent(fieldName, name -> new PropertyDescriptor(name, fieldType));
                if (!types.isSameType(property.getType(), fieldType)) {
                    throw new IntrospectionException(child, "Field type does not match property type");
                }
                property.setField(((VariableElement) child));
            } else if (child.getKind() == METHOD
                    && child.getModifiers().contains(PUBLIC)
            ) {
                final ExecutableElement methodElement = (ExecutableElement) child;
                final int isGetter = isGetter(methodElement);
                if (isGetter > 0) {
                    final String propertyName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, methodElement.getSimpleName().toString().substring(isGetter));
                    final TypeMirror propertyType = methodElement.getReturnType();
                    final var property = properties.computeIfAbsent(propertyName, name -> new PropertyDescriptor(name, propertyType));
                    if (!types.isSameType(property.getType(), propertyType)) {
                        throw new IntrospectionException(child, String.format("Getter type %s does not match property type %s", propertyType, property.getType()));
                    }
                    property.setGetter(methodElement);
                }
                if (isSetter(methodElement)) {
                    final String propertyName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, methodElement.getSimpleName().toString().substring(3));
                    final TypeMirror propertyType = methodElement.getParameters().get(0).asType();
                    final var property = properties.computeIfAbsent(propertyName, name -> new PropertyDescriptor(name, propertyType));
                    if (!types.isSameType(property.getType(), propertyType)) {
                        throw new IntrospectionException(child, "Setter type does not match property type");
                    }
                    property.setSetter(methodElement);
                }
            }
        }
        properties.values().forEach(this::checkIntegrity);
        return properties;
    }

    private void checkIntegrity(PropertyDescriptor property) {
        if (property.getField() != null && property.getField().getModifiers().contains(FINAL)
                && property.isWritable()) {
            throw new IntrospectionException(property.getField(), "Writeable property on final field");
        }
    }

    private int isGetter(ExecutableElement element) {
        final String name = element.getSimpleName().toString();
        if (!element.getParameters().isEmpty()) {
            return 0;
        }
        if (element.getReturnType().getKind() != TypeKind.VOID && name.startsWith("get")) {
            return 3;
        }
        if (element.getReturnType().getKind() == TypeKind.BOOLEAN && name.startsWith("is")) {
            return 2;
        }
        return 0;
    }

    private boolean isSetter(ExecutableElement element) {
        return element.getReturnType().getKind() == TypeKind.VOID
                && element.getParameters().size() == 1
                && element.getSimpleName().toString().startsWith("set");
    }

}
