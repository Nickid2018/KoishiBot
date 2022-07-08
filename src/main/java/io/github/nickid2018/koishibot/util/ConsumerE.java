package io.github.nickid2018.koishibot.util;

public interface ConsumerE<T> {

    void accept(T t) throws Exception;
}
