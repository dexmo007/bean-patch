package com.dexmohq.bean.patch.processor;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

public class PatchParameter {
    private final VariableElement parameter;
    private final PatchParameterType type;
    private final TypeMirror patchType;

    public PatchParameter(VariableElement parameter, PatchParameterType type, TypeMirror patchType) {
        this.parameter = parameter;
        this.type = type;
        this.patchType = patchType;
    }

    public VariableElement getParameter() {
        return parameter;
    }

    public PatchParameterType getType() {
        return type;
    }

    public TypeMirror getPatchType() {
        return patchType;
    }
}
