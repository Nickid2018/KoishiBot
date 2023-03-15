package io.github.nickid2018.koishibot.message.network;

import java.util.HashMap;
import java.util.Map;

public class DataRepository<T> {

    private final Map<String, T> dataStored = new HashMap<>();

    public void put(String key, T value) {
        dataStored.put(key, value);
    }

    public T get(String key) {
        return dataStored.get(key);
    }

    public void remove(String key) {
        dataStored.remove(key);
    }
}
