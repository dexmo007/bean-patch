package com.dexmohq.bean.patch.processor;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

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
}
