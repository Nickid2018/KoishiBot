package io.github.nickid2018.koishibot.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleThreadFactory implements ThreadFactory {

    private final AtomicInteger id = new AtomicInteger(0);
    private final String name;

    public SimpleThreadFactory(String name) {
        this.name = name;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, name.formatted(id.getAndIncrement()));
        thread.setDaemon(true);
        return thread;
    }
}
