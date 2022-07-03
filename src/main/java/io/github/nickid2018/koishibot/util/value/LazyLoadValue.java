package io.github.nickid2018.koishibot.util.value;

import java.util.function.Supplier;

public class LazyLoadValue<T> {

    private T value;
    private Supplier<T> valueSupplier;

    public LazyLoadValue(Supplier<T> valueSupplier) {
        this.valueSupplier = valueSupplier;
    }

    public T get() {
        if (valueSupplier != null) {
            value = valueSupplier.get();
            valueSupplier = null;
        }
        return value;
    }
}
