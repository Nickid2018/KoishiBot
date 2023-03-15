package io.github.nickid2018.koishibot.network;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class PacketRegistry {

    private final Object2IntMap<Class<? extends SerializableData>> packetMap = new Object2IntOpenHashMap<>();
    private final Map<Class<? extends SerializableData>,
            Function<Class<? extends SerializableData>, ? extends SerializableData>> packetFactory = new HashMap<>();
    private final AtomicInteger id = new AtomicInteger(0);

    public void registerPacket(Class<? extends SerializableData> packetClass) {
        packetMap.put(packetClass, id.getAndIncrement());
        packetFactory.put(packetClass, DEFAULT_FACTORY);
    }

    public void registerPacketFactory(Class<? extends SerializableData> packetClass,
                                      Function<Class<? extends SerializableData>, ? extends SerializableData> factory) {
        packetFactory.put(packetClass, factory);
    }

    public int getPacketId(Class<? extends SerializableData> packetClass) {
        return packetMap.getInt(packetClass);
    }

    public Class<? extends SerializableData> getPacketClass(int id) {
        return packetMap.keySet().stream().filter(c -> packetMap.getInt(c) == id).findFirst().orElse(null);
    }

    public SerializableData createPacket(Class<? extends SerializableData> packetClass) {
        return packetFactory.get(packetClass).apply(packetClass);
    }

    public static final Function<Class<? extends SerializableData>, ? extends SerializableData> DEFAULT_FACTORY = c -> {
        try {
            return c.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            return null;
        }
    };
}
