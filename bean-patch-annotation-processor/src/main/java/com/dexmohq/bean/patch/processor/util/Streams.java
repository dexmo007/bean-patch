package com.dexmohq.bean.patch.processor.util;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

public class Streams {

    @SafeVarargs
    public static <E> Stream<E> ofNullables(E... nullables) {
        return Arrays.stream(nullables)
                .map(Optional::ofNullable)
                .flatMap(Optional::stream);
    }
}
