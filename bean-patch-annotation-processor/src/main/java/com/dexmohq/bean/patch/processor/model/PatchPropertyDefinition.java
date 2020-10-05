package com.dexmohq.bean.patch.processor.model;

import com.dexmohq.bean.patch.processor.beans.PropertyDescriptor;
import com.dexmohq.bean.patch.spi.PatchType;

import javax.lang.model.element.ExecutableElement;

public class PatchPropertyDefinition {

    private final PropertyDescriptor patchProperty;
    private final PropertyDescriptor entityProperty;
    private final PatchType patchType;

    public PatchPropertyDefinition(PropertyDescriptor patchProperty, PropertyDescriptor entityProperty, PatchType patchType) {
        this.patchProperty = patchProperty;
        this.entityProperty = entityProperty;
        this.patchType = patchType;
    }

    public PropertyDescriptor getPatchProperty() {
        return patchProperty;
    }

    public PropertyDescriptor getEntityProperty() {
        return entityProperty;
    }

    public ExecutableElement getPatchReadMethod() {
        return patchProperty.getGetter();
    }

    public ExecutableElement getEntityWriteMethod() {
        return entityProperty.getSetter();
    }
    public ExecutableElement getEntityReadMethod() {
        return entityProperty.getGetter();
    }

    public PatchType getPatchType() {
        return patchType;
    }
}
