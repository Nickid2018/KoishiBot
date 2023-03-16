package io.github.nickid2018.koishibot.message.network;

import io.github.nickid2018.koishibot.message.event.QueryResultEvent;
import io.github.nickid2018.koishibot.message.query.Query;
import io.github.nickid2018.koishibot.network.Connection;
import io.github.nickid2018.koishibot.network.NetworkListener;
import io.github.nickid2018.koishibot.network.SerializableData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DataPacketListener implements NetworkListener {

    private ExecutorService queryExecutor;
    private final Map<UUID, CompletableFuture<byte[]>> queryResults = new ConcurrentHashMap<>();

    public CompletableFuture<byte[]> queryData(Connection connection, Query query) {
        CompletableFuture<byte[]> future = new CompletableFuture<>();
        queryResults.put(query.queryId, future);
        connection.sendPacket(query);
        return future;
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
            queryResults.get(queryId).complete(result);
        }
    }

    @Override
    public void connectionClosed(Connection connection) {
        queryExecutor.shutdownNow();
    }
}
