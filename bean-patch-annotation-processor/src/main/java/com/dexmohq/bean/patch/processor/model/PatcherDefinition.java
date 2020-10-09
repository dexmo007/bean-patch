package com.dexmohq.bean.patch.processor.model;

import com.dexmohq.bean.patch.processor.gen.ComponentModel;

import javax.lang.model.element.TypeElement;
import java.util.List;

public class PatcherDefinition {

    private TypeElement origin;

    private ComponentModel componentModel;

    private List<PatchMethod> patchMethods;

    public TypeElement getOrigin() {
        return origin;
    }

    public void setOrigin(TypeElement origin) {
        this.origin = origin;
    }

    public ComponentModel getComponentModel() {
        return componentModel;
    }

    public void setComponentModel(ComponentModel componentModel) {
        this.componentModel = componentModel;
    }

    public List<PatchMethod> getPatchMethods() {
        return patchMethods;
    }

    public void setPatchMethods(List<PatchMethod> patchMethods) {
        this.patchMethods = patchMethods;
    }
}
