package io.github.nickid2018.koishibot.util;

import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.function.FailableFunction;

import java.util.function.Consumer;
import java.util.function.Function;

@InternalStack
public class FuncUtils {

    public static <T, E extends Throwable> Consumer<T> rethrow(FailableConsumer<T, E> consumer) {
        return t -> {
            try {
                consumer.accept(t);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static <T, R, E extends Throwable> Function<T, R> rethrow(FailableFunction<T, R, E> func) {
        return t -> {
            try {
                return func.apply(t);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        };
    }
}
