package io.github.nickid2018.koishibot.util.func;

import java.util.function.Consumer;
import java.util.function.Function;

public class FuncUtils {

    public static <T> Consumer<T> rethrow(ConsumerNE<T> consumer) {
        return t -> {
            try {
                consumer.accept(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static <T, R> Function<T, R> rethrow(FunctionNE<T, R> func) {
        return t -> {
            try {
                return func.apply(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}
