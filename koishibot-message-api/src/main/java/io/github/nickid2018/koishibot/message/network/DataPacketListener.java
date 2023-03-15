package io.github.nickid2018.koishibot.message.network;

import io.github.nickid2018.koishibot.message.event.QueryResultEvent;
import io.github.nickid2018.koishibot.network.Connection;
import io.github.nickid2018.koishibot.network.NetworkListener;
import io.github.nickid2018.koishibot.network.SerializableData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class DataPacketListener implements NetworkListener {

    private ExecutorService queryExecutor;
    private final Map<UUID, byte[]> queryResults = new ConcurrentHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public Future<byte[]> queryData(UUID queryId) {
        return queryExecutor.submit(() -> {
            while (!closed.get()) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ignored) {
                }
                if (queryResults.containsKey(queryId)) {
                    byte[] result = queryResults.get(queryId);
                    queryResults.remove(queryId);
                    return result;
                }
            }
            return null;
        });
    }

    @Override
    public void connectionOpened(Connection connection) {
        queryExecutor = Executors.newFixedThreadPool(10);
    }

    @Override
    public void receivePacket(Connection connection, SerializableData packet) {
        if (packet instanceof QueryResultEvent queryResultEvent) {
            UUID queryId = queryResultEvent.queryId;
            byte[] result = queryResultEvent.payload;
            queryResults.put(queryId, result);
        }
    }

    @Override
    public void connectionClosed(Connection connection) {
        closed.set(true);
        queryExecutor.shutdownNow();
    }
}
