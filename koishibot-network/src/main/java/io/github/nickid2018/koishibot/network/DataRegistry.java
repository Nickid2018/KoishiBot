package io.github.nickid2018.koishibot.network;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class DataRegistry {

    private final Object2IntMap<Class<? extends SerializableData>> dataMap = new Object2IntOpenHashMap<>();
    private final Map<Class<? extends SerializableData>,
            Function<Class<? extends SerializableData>, ? extends SerializableData>> dataFactory = new HashMap<>();
    private final AtomicInteger id = new AtomicInteger(0);

    public DataRegistry() {
        registerData(NullData.class, s -> NullData.INSTANCE);
    }

    public void registerData(Class<? extends SerializableData> dataClass) {
        dataMap.put(dataClass, id.getAndIncrement());
        dataFactory.put(dataClass, DEFAULT_FACTORY);
    }

    public void registerData(Class<? extends SerializableData> dataClass,
                             Function<Class<? extends SerializableData>, ? extends SerializableData> factory) {
        dataMap.put(dataClass, id.getAndIncrement());
        dataFactory.put(dataClass, factory);
    }

    public int getPacketId(Class<? extends SerializableData> dataClass) {
        return dataMap.getInt(dataClass);
    }

    public Class<? extends SerializableData> getDataClass(int id) {
        return dataMap.keySet().stream().filter(c -> dataMap.getInt(c) == id).findFirst().orElse(null);
    }

    public SerializableData createData(Class<? extends SerializableData> dataClass) {
        return dataFactory.get(dataClass).apply(dataClass);
    }

    public static final Function<Class<? extends SerializableData>, ? extends SerializableData> DEFAULT_FACTORY = c -> {
        try {
            return c.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            return null;
        }
    };
}
