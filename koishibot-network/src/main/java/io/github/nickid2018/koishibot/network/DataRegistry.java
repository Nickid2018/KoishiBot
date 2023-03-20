package io.github.nickid2018.koishibot.network;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

public class DataRegistry {

    private final Object2IntMap<Class<? extends SerializableData>> dataMap = new Object2IntOpenHashMap<>();
    private final Map<Class<? extends SerializableData>,
            BiFunction<Class<? extends SerializableData>, Connection, ? extends SerializableData>> dataFactory = new HashMap<>();
    private final AtomicInteger id = new AtomicInteger(0);

    public DataRegistry() {
        registerData(NullData.class, (s, cn) -> NullData.INSTANCE);
        registerData(StringData.class, (s, cn) -> new StringData());
    }

    public void registerData(Class<? extends SerializableData> dataClass) {
        dataMap.put(dataClass, id.getAndIncrement());
        dataFactory.put(dataClass, DEFAULT_FACTORY);
    }

    public void registerData(Class<? extends SerializableData> dataClass,
                             BiFunction<Class<? extends SerializableData>, Connection, ? extends SerializableData> factory) {
        dataMap.put(dataClass, id.getAndIncrement());
        dataFactory.put(dataClass, factory);
    }

    public int getPacketId(Class<? extends SerializableData> dataClass) {
        List<Class<? extends SerializableData>> list = dataMap.keySet().stream().filter(c -> c.isAssignableFrom(dataClass)).toList();
        return list.isEmpty() ? -1 : dataMap.getInt(list.get(0));
    }

    public Class<? extends SerializableData> getDataClass(int id) {
        return dataMap.keySet().stream().filter(c -> dataMap.getInt(c) == id).findFirst().orElse(null);
    }

    public SerializableData createData(Connection connection, Class<? extends SerializableData> dataClass) {
        return dataFactory.get(dataClass).apply(dataClass, connection);
    }

    public static final BiFunction<Class<? extends SerializableData>, Connection, ? extends SerializableData> DEFAULT_FACTORY = (c, cn) -> {
        try {
            return c.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            return null;
        }
    };
}
