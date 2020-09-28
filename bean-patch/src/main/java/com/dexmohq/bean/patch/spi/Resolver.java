package com.dexmohq.bean.patch.spi;

public interface Resolver<S, T> {

    T resolve(S source);

}
