package com.dexmohq.bean.patch.processor.util;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public final class MoreCollectors {

    public static <E> Collector<E, ?, Optional<E>> toSingleton(Supplier<? extends RuntimeException> thrower) {
        return Collectors.collectingAndThen(Collectors.toList(), list -> {
            if (list.isEmpty()) {
                return Optional.empty();
            }
            if (list.size() == 1) {
                return Optional.ofNullable(list.get(0));
            }
            throw thrower.get();
        });
    }

    public static <E> Collector<E, ?, Optional<E>> toSingleton() {
        return toSingleton(() -> new IllegalStateException("Expected exactly 1 element in stream"));
    }

    private MoreCollectors() {
        throw new UnsupportedOperationException();
    }

}
