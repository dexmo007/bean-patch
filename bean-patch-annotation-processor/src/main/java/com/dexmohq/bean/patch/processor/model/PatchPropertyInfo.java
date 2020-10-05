package com.dexmohq.bean.patch.processor.model;

import com.dexmohq.bean.patch.processor.beans.PropertyDescriptor;
import com.dexmohq.bean.patch.spi.PatchProperty;
import com.dexmohq.bean.patch.spi.PatchType;

public class PatchPropertyInfo {

    private final String target;
    private final PatchType type;


    public PatchPropertyInfo(String target, PatchType type) {
        this.target = target;
        this.type = type;
    }

    public String getTarget() {
        return target;
    }

    public PatchType getType() {
        return type;
    }

    public static PatchPropertyInfo from(PropertyDescriptor prop, PatchProperty patchProperty) {
        if (patchProperty == null) {
            return defaultFor(prop);
        }
        final String target = patchProperty.value().isEmpty() ? prop.getName() : patchProperty.value();
        return new PatchPropertyInfo(target, patchProperty.type());
    }

    public static PatchPropertyInfo defaultFor(PropertyDescriptor prop) {
        return new PatchPropertyInfo(prop.getName(), PatchType.SET);
    }
}
