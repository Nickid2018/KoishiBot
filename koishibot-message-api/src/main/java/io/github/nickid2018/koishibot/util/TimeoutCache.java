package io.github.nickid2018.koishibot.util;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TimeoutCache<T> {

    private final Map<String, Long> lifeTime = new ConcurrentHashMap<>();
    private final Map<String, T> cache = new ConcurrentHashMap<>();

    private void checkTimeout() {
        Set<String> outdated = lifeTime.keySet().stream()
                .filter(key -> lifeTime.get(key) < System.currentTimeMillis())
                .collect(Collectors.toSet());
        outdated.forEach(cache::remove);
        outdated.forEach(lifeTime::remove);
    }

    public void put(String key, T value, long timeout) {
        checkTimeout();
        cache.put(key, value);
        lifeTime.put(key, System.currentTimeMillis() + timeout);
    }

    public T get(String key) {
        return cache.get(key);
    }

    public void remove(String key) {
        cache.remove(key);
        lifeTime.remove(key);
    }

    public void clear() {
        cache.clear();
        lifeTime.clear();
    }

    public boolean containsKey(String key) {
        checkTimeout();
        return cache.containsKey(key);
    }
}
