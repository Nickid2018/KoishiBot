package io.github.nickid2018.koishibot.message.network;

import io.github.nickid2018.koishibot.message.action.NudgeAction;
import io.github.nickid2018.koishibot.message.action.RecallAction;
import io.github.nickid2018.koishibot.message.query.SendMessageQuery;
import io.github.nickid2018.koishibot.message.action.StopAction;
import io.github.nickid2018.koishibot.message.api.*;
import io.github.nickid2018.koishibot.message.event.*;
import io.github.nickid2018.koishibot.message.query.GroupInfoQuery;
import io.github.nickid2018.koishibot.message.query.NameInGroupQuery;
import io.github.nickid2018.koishibot.message.query.Query;
import io.github.nickid2018.koishibot.message.query.UserInfoQuery;
import io.github.nickid2018.koishibot.network.Connection;
import io.github.nickid2018.koishibot.network.DataRegistry;
import io.github.nickid2018.koishibot.network.NetworkListener;
import io.github.nickid2018.koishibot.network.SerializableData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;

public class DataPacketListener implements NetworkListener {

    private ExecutorService queryExecutor;
    private final Map<UUID, CompletableFuture<byte[]>> queryResults = new ConcurrentHashMap<>();

    protected final DataRegistry registry = new DataRegistry();

    public DataPacketListener(BiFunction<Class<? extends SerializableData>, Connection, ? extends SerializableData> dataFactory) {
        registry.registerData(Environment.class, dataFactory);
        registry.registerData(MessageContext.class, dataFactory);

        registry.registerData(AtMessage.class, dataFactory);
        registry.registerData(AudioMessage.class, dataFactory);
        registry.registerData(ChainMessage.class, dataFactory);
        registry.registerData(ForwardMessage.class, dataFactory);
        registry.registerData(GroupInfo.class, dataFactory);
        registry.registerData(ImageMessage.class, dataFactory);
        registry.registerData(MessageEntry.class, dataFactory);
        registry.registerData(MessageSource.class, dataFactory);
        registry.registerData(QuoteMessage.class, dataFactory);
        registry.registerData(ServiceMessage.class, dataFactory);
        registry.registerData(TextMessage.class, dataFactory);
        registry.registerData(UnsupportedMessage.class, dataFactory);
        registry.registerData(UserInfo.class, dataFactory);

        registry.registerData(QueryResultEvent.class, dataFactory);
        registry.registerData(OnFriendMessageEvent.class, dataFactory);
        registry.registerData(OnFriendRecallEvent.class, dataFactory);
        registry.registerData(OnGroupMessageEvent.class, dataFactory);
        registry.registerData(OnGroupRecallEvent.class, dataFactory);
        registry.registerData(OnMemberAddEvent.class, dataFactory);
        registry.registerData(OnStrangerMessageEvent.class, dataFactory);

        registry.registerData(GroupInfoQuery.class, dataFactory);
        registry.registerData(NameInGroupQuery.class, dataFactory);
        registry.registerData(UserInfoQuery.class, dataFactory);
        registry.registerData(SendMessageQuery.class, dataFactory);

        registry.registerData(NudgeAction.class, dataFactory);
        registry.registerData(RecallAction.class, dataFactory);
        registry.registerData(StopAction.class, (c, cn) -> StopAction.INSTANCE);
    }

    public DataRegistry getRegistry() {
        return registry;
    }

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
