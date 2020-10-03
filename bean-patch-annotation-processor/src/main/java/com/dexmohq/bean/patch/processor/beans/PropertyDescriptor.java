package com.dexmohq.bean.patch.processor.beans;

import com.dexmohq.bean.patch.processor.util.Streams;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.Objects;
import java.util.Optional;

import static com.dexmohq.bean.patch.processor.util.MoreCollectors.toSingleton;

public class PropertyDescriptor {
    private final String name;
    private final TypeMirror type;

    private VariableElement field;
    private ExecutableElement getter;
    private ExecutableElement setter;

    public PropertyDescriptor(String name, TypeMirror type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public TypeMirror getType() {
        return type;
    }

    public VariableElement getField() {
        return field;
    }

    public void setField(VariableElement field) {
        this.field = field;
    }

    public ExecutableElement getGetter() {
        return getter;
    }

    public void setGetter(ExecutableElement getter) {
        this.getter = getter;
    }

    public ExecutableElement getSetter() {
        return setter;
    }

    public void setSetter(ExecutableElement setter) {
        this.setter = setter;
    }

    public <A extends Annotation> Optional<A> getAnnotation(Class<A> annotationType) {
        return Streams.ofNullables(field, getter, setter)
                .map(e -> e.getAnnotation(annotationType))
                .filter(Objects::nonNull)
                .collect(toSingleton())
                ;
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return Streams.ofNullables(field, getter, setter)
                .anyMatch(e -> e.getAnnotation(annotationType) != null);
    }

    public boolean isWritable() {
        return setter != null;
    }

    public boolean isReadable() {
        return getter == null;
    }

}
