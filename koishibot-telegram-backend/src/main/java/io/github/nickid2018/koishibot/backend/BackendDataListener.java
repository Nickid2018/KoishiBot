package io.github.nickid2018.koishibot.backend;

import io.github.nickid2018.koishibot.message.action.RecallAction;
import io.github.nickid2018.koishibot.message.action.StopAction;
import io.github.nickid2018.koishibot.message.api.*;
import io.github.nickid2018.koishibot.message.event.QueryResultEvent;
import io.github.nickid2018.koishibot.message.network.DataPacketListener;
import io.github.nickid2018.koishibot.message.query.GroupInfoQuery;
import io.github.nickid2018.koishibot.message.query.NameInGroupQuery;
import io.github.nickid2018.koishibot.message.query.SendMessageQuery;
import io.github.nickid2018.koishibot.message.query.UserInfoQuery;
import io.github.nickid2018.koishibot.message.telegram.*;
import io.github.nickid2018.koishibot.network.Connection;
import io.github.nickid2018.koishibot.network.SerializableData;
import io.github.nickid2018.koishibot.util.Either;
import io.github.nickid2018.koishibot.util.LogUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class BackendDataListener extends DataPacketListener {

    private final Supplier<TelegramEnvironment> environment;
    private final CompletableFuture<Void> disconnectFuture;

    public static final Map<Class<? extends SerializableData>, Class<? extends SerializableData>> MAPPING = new HashMap<>();

    static {
        MAPPING.put(AtMessage.class, TelegramAt.class);
        MAPPING.put(AudioMessage.class, TelegramAudio.class);
        MAPPING.put(ChainMessage.class, TelegramChain.class);
        MAPPING.put(GroupInfo.class, TelegramGroup.class);
        MAPPING.put(ImageMessage.class, TelegramImage.class);
        MAPPING.put(MessageSource.class, TelegramMessageSource.class);
        MAPPING.put(QuoteMessage.class, TelegramQuote.class);
        MAPPING.put(TextMessage.class, TelegramText.class);
        MAPPING.put(UserInfo.class, TelegramUser.class);
    }

    public BackendDataListener(Supplier<TelegramEnvironment> environment, CompletableFuture<Void> disconnectFuture) {
        super((c, cn) -> {
            try {
                if (c.equals(Environment.class))
                    return null;
                if (MAPPING.containsKey(c))
                    return MAPPING.get(c).getConstructor(TelegramEnvironment.class).newInstance(environment.get());
                return c.getConstructor(Environment.class).newInstance(environment.get());
            } catch (Exception e) {
                return null;
            }
        });
        this.environment = environment;
        this.disconnectFuture = disconnectFuture;
    }

    @Override
    public void connectionOpened(Connection connection) {
        super.connectionOpened(connection);
        connection.sendPacket(environment.get());
        LogUtils.info(LogUtils.FontColor.GREEN, Main.LOGGER, "Connected to core");
    }

    @Override
    public void receivePacket(Connection connection, SerializableData packet) {
        super.receivePacket(connection, packet);
        if (packet instanceof GroupInfoQuery groupInfoQuery)
            groupInfoQuery(connection, groupInfoQuery);
        else if (packet instanceof NameInGroupQuery nameInGroupQuery)
            nameInGroupQuery(connection, nameInGroupQuery);
        else if (packet instanceof UserInfoQuery userInfoQuery)
            userInfoQuery(connection, userInfoQuery);
        else if (packet instanceof SendMessageQuery action)
            sendMessageQuery(connection, action);
        else if (packet instanceof RecallAction action)
            recallAction(connection, action);
        else if (packet instanceof StopAction)
            doStop();
    }

    @Override
    public void connectionClosed(Connection connection) {
        super.connectionClosed(connection);
        disconnectFuture.complete(null);
    }

    private void groupInfoQuery(Connection connection, GroupInfoQuery query) {
        GroupInfo info = environment.get().getGroup(query.id);
        if (info == null) {
            info = new GroupInfo(environment.get());
            info.groupId = query.id;
            info.name = "Unknown";
        }
        QueryResultEvent event = new QueryResultEvent(environment.get());
        event.queryId = query.queryId;
        event.payload = GroupInfoQuery.toBytes(info);
        connection.sendPacket(event);
    }

    private void userInfoQuery(Connection connection, UserInfoQuery query) {
        UserInfo info = environment.get().getUser(query.id, query.isStranger);
        if (info == null) {
            info = new UserInfo(environment.get());
            info.userId = query.id;
            info.name = "Unknown";
            info.isStranger = query.isStranger;
        }
        QueryResultEvent event = new QueryResultEvent(environment.get());
        event.queryId = query.queryId;
        event.payload = UserInfoQuery.toBytes(info);
        connection.sendPacket(event);
    }

    private void nameInGroupQuery(Connection connection, NameInGroupQuery query) {
        String info = TelegramUser.getNameInGroup(query.user, query.group);
        QueryResultEvent event = new QueryResultEvent(environment.get());
        event.queryId = query.queryId;
        event.payload = info.getBytes(StandardCharsets.UTF_8);
        connection.sendPacket(event);
    }

    private void sendMessageQuery(Connection connection, SendMessageQuery action) {
        Either<UserInfo, GroupInfo> contact = action.target;
        if (contact.isRight())
            TelegramMessage.send((TelegramMessage) action.message, contact.right());
        QueryResultEvent event = new QueryResultEvent(environment.get());
        event.queryId = action.queryId;
        event.payload = SendMessageQuery.toBytes(connection, action.message.getSource());
        connection.sendPacket(event);
    }

    private void recallAction(Connection connection, RecallAction action) {
        if (TelegramMessageSource.messageCache.containsKey(action.messageUniqueID)) {
            TelegramMessageSource.recall(TelegramMessageSource.messageCache.get(action.messageUniqueID));
            TelegramMessageSource.messageCache.remove(action.messageUniqueID);
        }
    }

    private void doStop() {
        Main.stopped.set(true);
        environment.get().close();
        disconnectFuture.complete(null);
    }
}
