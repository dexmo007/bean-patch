package com.dexmohq.bean.patch.spi;

public interface PatchPropertyContext<E, P, S, T> {

    E getEntity();

    P getPatch();

    S getSourceValue();

    void setTargetValue(T targetValue);

}
