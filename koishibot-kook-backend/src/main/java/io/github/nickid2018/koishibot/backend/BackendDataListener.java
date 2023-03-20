package io.github.nickid2018.koishibot.backend;

import io.github.nickid2018.koishibot.message.action.NudgeAction;
import io.github.nickid2018.koishibot.message.action.RecallAction;
import io.github.nickid2018.koishibot.message.action.SendMessageAction;
import io.github.nickid2018.koishibot.message.action.StopAction;
import io.github.nickid2018.koishibot.message.api.*;
import io.github.nickid2018.koishibot.message.event.*;
import io.github.nickid2018.koishibot.message.kook.*;
import io.github.nickid2018.koishibot.message.network.DataPacketListener;
import io.github.nickid2018.koishibot.message.query.GroupInfoQuery;
import io.github.nickid2018.koishibot.message.query.NameInGroupQuery;
import io.github.nickid2018.koishibot.message.query.UserInfoQuery;
import io.github.nickid2018.koishibot.network.Connection;
import io.github.nickid2018.koishibot.network.DataRegistry;
import io.github.nickid2018.koishibot.network.SerializableData;
import io.github.nickid2018.koishibot.util.Either;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class BackendDataListener extends DataPacketListener {

    public final DataRegistry registry = new DataRegistry();
    private final Supplier<KOOKEnvironment> environment;
    private final CompletableFuture<Void> disconnectFuture;

    public BackendDataListener(Supplier<KOOKEnvironment> environment, CompletableFuture<Void> disconnectFuture) {
        this.environment = environment;
        this.disconnectFuture = disconnectFuture;
        BiFunction<Class<? extends SerializableData>, Connection, ? extends SerializableData> dataFactory = (c, cn) -> {
            try {
                return (c.getTypeName().contains("kook") ?
                        c.getConstructor(KOOKEnvironment.class) :
                        c.getConstructor(Environment.class)).newInstance(environment.get());
            } catch (Exception e) {
                return null;
            }
        };

        registry.registerData(KOOKEnvironment.class, (c, cn) -> null);

        registry.registerData(KOOKAt.class, dataFactory);
        registry.registerData(AudioMessage.class, dataFactory);
        registry.registerData(KOOKChain.class, dataFactory);
        registry.registerData(ForwardMessage.class, dataFactory);
        registry.registerData(KOOKTextChannel.class, dataFactory);
        registry.registerData(KOOKImage.class, dataFactory);
        registry.registerData(MessageEntry.class, dataFactory);
        registry.registerData(KOOKMessageSource.class, dataFactory);
        registry.registerData(KOOKQuote.class, dataFactory);
        registry.registerData(ServiceMessage.class, dataFactory);
        registry.registerData(KOOKText.class, dataFactory);
        registry.registerData(KOOKUser.class, dataFactory);

        registry.registerData(QueryResultEvent.class, dataFactory);
        registry.registerData(OnFriendMessageEvent.class, dataFactory);
        registry.registerData(OnFriendMessageEvent.class, dataFactory);
        registry.registerData(OnGroupMessageEvent.class, dataFactory);
        registry.registerData(OnGroupRecallEvent.class, dataFactory);
        registry.registerData(OnMemberAddEvent.class, dataFactory);
        registry.registerData(OnStrangerMessageEvent.class, dataFactory);

        registry.registerData(GroupInfoQuery.class, dataFactory);
        registry.registerData(NameInGroupQuery.class, dataFactory);
        registry.registerData(UserInfoQuery.class, dataFactory);

        registry.registerData(NudgeAction.class, dataFactory);
        registry.registerData(RecallAction.class, dataFactory);
        registry.registerData(SendMessageAction.class, dataFactory);
        registry.registerData(StopAction.class, (c, cn) -> StopAction.INSTANCE);
    }

    @Override
    public void connectionOpened(Connection connection) {
        super.connectionOpened(connection);
        connection.sendPacket(environment.get());
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
        else if (packet instanceof RecallAction action)
            recallAction(connection, action);
        else if (packet instanceof SendMessageAction action)
            sendMessageAction(connection, action);
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
        String info = KOOKUser.getNameInGroup(((KOOKUser) query.user).getUser(), query.group);
        QueryResultEvent event = new QueryResultEvent(environment.get());
        event.queryId = query.queryId;
        event.payload = info.getBytes(StandardCharsets.UTF_8);
        connection.sendPacket(event);
    }

    private void recallAction(Connection connection, RecallAction action) {
        if (KOOKMessageSource.messageCache.containsKey(action.messageUniqueID)) {
            KOOKMessageSource.messageCache.get(action.messageUniqueID).recall();
            KOOKMessageSource.messageCache.remove(action.messageUniqueID);
        }
    }

    private void sendMessageAction(Connection connection, SendMessageAction action) {
        Either<UserInfo, GroupInfo> contact = action.target;
        if (contact.isRight())
            KOOKMessage.send((KOOKMessage) action.message, contact.right());
    }

    private void doStop() {
        Main.stopped.set(true);
        environment.get().close();
    }
}
