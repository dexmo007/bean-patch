package com.dexmohq.bean.patch.spi;

public interface PropertyPatcher<E, P extends Patch<E>, S, T> {

    void patchProperty(PatchPropertyContext<E, P, S, T> context);

}
