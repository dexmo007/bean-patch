package com.dexmohq.bean.patch.processor;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.Map;
import java.util.Set;

class PatchMethodBuilder {
    private ExecutableElement element;
    private DeclaredType entityType;
    private int entityParameterIndex;
    private VariableElement entityParameter;
    private Map<Integer, PatchParameter> patchParameters;
    private Set<DeclaredType> patchTypes;

    public PatchMethodBuilder element(ExecutableElement element) {
        this.element = element;
        return this;
    }

    public PatchMethodBuilder entityType(DeclaredType entityType) {
        this.entityType = entityType;
        return this;
    }

    public PatchMethodBuilder entityParameterIndex(int entityParameterIndex) {
        this.entityParameterIndex = entityParameterIndex;
        return this;
    }

    public PatchMethodBuilder entityParameter(VariableElement entityParameter) {
        this.entityParameter = entityParameter;
        return this;
    }

    public PatchMethodBuilder patchParameters(Map<Integer, PatchParameter> patchParameters) {
        this.patchParameters = patchParameters;
        return this;
    }

    public PatchMethodBuilder patchTypes(Set<DeclaredType> patchTypes) {
        this.patchTypes = patchTypes;
        return this;
    }

    public PatchMethod build() {
        return new PatchMethod(element, entityType, entityParameterIndex, entityParameter, patchParameters, patchTypes);
    }
}