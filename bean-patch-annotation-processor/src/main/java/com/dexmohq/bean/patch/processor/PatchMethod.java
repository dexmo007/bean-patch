package com.dexmohq.bean.patch.processor;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import java.util.Map;
import java.util.Set;

public class PatchMethod {
    private final ExecutableElement element;
    private final DeclaredType entityType;
    private final int entityParameterIndex;
    private final VariableElement entityParameter;
    private final Map<Integer, PatchParameter> patchParameters;
    private final Set<DeclaredType> patchTypes;

    public PatchMethod(ExecutableElement element, DeclaredType entityType, int entityParameterIndex, VariableElement entityParameter, Map<Integer, PatchParameter> patchParameters, Set<DeclaredType> patchTypes) {
        this.element = element;
        this.entityType = entityType;
        this.entityParameterIndex = entityParameterIndex;
        this.entityParameter = entityParameter;
        this.patchParameters = patchParameters;
        this.patchTypes = patchTypes;
    }

    public ExecutableElement getElement() {
        return element;
    }

    public DeclaredType getEntityType() {
        return entityType;
    }

    public int getEntityParameterIndex() {
        return entityParameterIndex;
    }

    public VariableElement getEntityParameter() {
        return entityParameter;
    }

    public Map<Integer, PatchParameter> getPatchParameters() {
        return patchParameters;
    }

    public Set<DeclaredType> getPatchTypes() {
        return patchTypes;
    }

    public static PatchMethodBuilder builder() {
        return new PatchMethodBuilder();
    }
}
