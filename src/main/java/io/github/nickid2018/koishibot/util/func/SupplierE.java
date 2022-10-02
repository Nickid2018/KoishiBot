package io.github.nickid2018.koishibot.util.func;

public interface SupplierE<T, E extends Throwable> {

    T get() throws E;
}
