package com.dexmohq.bean.patch;

import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public class Patchers {

    public static <I> I getPatcher(Class<I> patcherType) {
        final ServiceLoader<I> serviceLoader = ServiceLoader.load(patcherType);
        final List<ServiceLoader.Provider<I>> serviceProviders = serviceLoader.stream().collect(Collectors.toList());
        if (serviceProviders.isEmpty()) {
            throw new IllegalStateException("No implementation found for patcher interface: " + patcherType);
        }
        if (serviceProviders.size() > 1) {
            throw new IllegalStateException("Multiple implementations found for patcher interface: " + patcherType);
        }
        final ServiceLoader.Provider<I> serviceProvider = serviceProviders.iterator().next();
        return serviceProvider.get();
    }

}
