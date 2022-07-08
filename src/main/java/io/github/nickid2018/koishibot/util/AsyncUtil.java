package io.github.nickid2018.koishibot.util;

import io.github.nickid2018.koishibot.core.ErrorRecord;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AsyncUtil {

    private static ExecutorService executor;

    public static void start() {
        executor = Executors.newCachedThreadPool(new BasicThreadFactory.Builder().uncaughtExceptionHandler(
                (th, t) -> ErrorRecord.enqueueError("concurrent", t)
        ).daemon(true).namingPattern("Async Worker %d").build());
    }

    public static void execute(Runnable runnable) {
        if (executor != null)
            executor.execute(runnable);
    }

    public static <V> Future<V> submit(Callable<V> callable) {
        return executor != null ? executor.submit(callable) : null;
    }

    public static void terminate() {
        executor.shutdown();
        executor = null;
    }
}
