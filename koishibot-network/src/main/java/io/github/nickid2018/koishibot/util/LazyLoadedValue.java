package io.github.nickid2018.koishibot.util;

import java.util.function.Supplier;

public class LazyLoadedValue<T> {

    private T value;
    private final Supplier<T> loader;

    public LazyLoadedValue(Supplier<T> loader) {
        this.loader = loader;
    }

    public T get() {
        if (value == null) {
            value = loader.get();
        }
        return value;
    }
}
