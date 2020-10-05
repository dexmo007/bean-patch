package com.dexmohq.bean.patch.processor.model;

import javax.lang.model.type.DeclaredType;
import java.util.ArrayList;
import java.util.List;

public class PatchDefinition {

    private final DeclaredType entityType;
    private final DeclaredType patchType;
    private final List<PatchPropertyDefinition> propertyDefinitions = new ArrayList<>();

    public PatchDefinition(PatchTypePair pair) {
        this.entityType = pair.getEntityType();
        this.patchType = pair.getPatchType();
    }

    public void addPatch(PatchPropertyDefinition def) {
        propertyDefinitions.add(def);
    }

    public DeclaredType getEntityType() {
        return entityType;
    }

    public DeclaredType getPatchType() {
        return patchType;
    }

    public List<PatchPropertyDefinition> getPropertyDefinitions() {
        return propertyDefinitions;
    }
}
