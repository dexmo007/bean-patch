package com.dexmohq.bean.patch.processor;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.Objects;

public class PatchTypePair implements TypeMirrorLike {

    private final DeclaredType entityType;
    private final DeclaredType patchType;

    public PatchTypePair(DeclaredType entityType, DeclaredType patchType) {
        this.entityType = Objects.requireNonNull(entityType);
        this.patchType = Objects.requireNonNull(patchType);
    }

    public DeclaredType getEntityType() {
        return entityType;
    }

    public DeclaredType getPatchType() {
        return patchType;
    }

    public TypeElement getEntityTypeElement() {
        return ((TypeElement) entityType.asElement());
    }

    public TypeElement getPatchTypeElement() {
        return ((TypeElement) patchType.asElement());
    }

    @Override
    public TypeMirror[] getTypeMirrors() {
        return new TypeMirror[]{entityType, patchType};
    }
}
