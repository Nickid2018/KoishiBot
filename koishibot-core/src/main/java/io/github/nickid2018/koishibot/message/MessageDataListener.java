package io.github.nickid2018.koishibot.message;

import io.github.nickid2018.koishibot.message.action.NudgeAction;
import io.github.nickid2018.koishibot.message.action.RecallAction;
import io.github.nickid2018.koishibot.message.action.SendMessageAction;
import io.github.nickid2018.koishibot.message.action.StopAction;
import io.github.nickid2018.koishibot.message.api.*;
import io.github.nickid2018.koishibot.message.event.*;
import io.github.nickid2018.koishibot.message.network.DataPacketListener;
import io.github.nickid2018.koishibot.message.query.GroupInfoQuery;
import io.github.nickid2018.koishibot.message.query.NameInGroupQuery;
import io.github.nickid2018.koishibot.message.query.UserInfoQuery;
import io.github.nickid2018.koishibot.network.Connection;
import io.github.nickid2018.koishibot.network.DataRegistry;
import io.github.nickid2018.koishibot.network.SerializableData;

import java.util.function.BiFunction;

public class MessageDataListener extends DataPacketListener {

    public static final DataRegistry REGISTRY = new DataRegistry();

    static {
        BiFunction<Class<? extends SerializableData>, Connection, ? extends SerializableData> dataFactory = (c, cn) -> {
            try {
                return c.getConstructor(Environment.class).newInstance(Environments.getEnvironment(cn));
            } catch (Exception e) {
                return null;
            }
        };

        REGISTRY.registerData(DelegateEnvironment.class, (c, cn) -> {
            DelegateEnvironment env = new DelegateEnvironment(cn);
            Environments.putEnvironment(env.getEnvironmentName(), env);
            return env;
        });

        REGISTRY.registerData(AtMessage.class, dataFactory);
        REGISTRY.registerData(AudioMessage.class, dataFactory);
        REGISTRY.registerData(ChainMessage.class, dataFactory);
        REGISTRY.registerData(ForwardMessage.class, dataFactory);
        REGISTRY.registerData(GroupInfo.class, dataFactory);
        REGISTRY.registerData(ImageMessage.class, dataFactory);
        REGISTRY.registerData(MessageEntry.class, dataFactory);
        REGISTRY.registerData(MessageSource.class, dataFactory);
        REGISTRY.registerData(QuoteMessage.class, dataFactory);
        REGISTRY.registerData(ServiceMessage.class, dataFactory);
        REGISTRY.registerData(TextMessage.class, dataFactory);
        REGISTRY.registerData(UserInfo.class, dataFactory);

        REGISTRY.registerData(QueryResultEvent.class, dataFactory);
        REGISTRY.registerData(OnFriendMessageEvent.class, dataFactory);
        REGISTRY.registerData(OnFriendMessageEvent.class, dataFactory);
        REGISTRY.registerData(OnGroupMessageEvent.class, dataFactory);
        REGISTRY.registerData(OnGroupRecallEvent.class, dataFactory);
        REGISTRY.registerData(OnMemberAddEvent.class, dataFactory);
        REGISTRY.registerData(OnStrangerMessageEvent.class, dataFactory);

        REGISTRY.registerData(GroupInfoQuery.class, dataFactory);
        REGISTRY.registerData(NameInGroupQuery.class, dataFactory);
        REGISTRY.registerData(UserInfoQuery.class, dataFactory);

        REGISTRY.registerData(NudgeAction.class, dataFactory);
        REGISTRY.registerData(RecallAction.class, dataFactory);
        REGISTRY.registerData(SendMessageAction.class, dataFactory);
        REGISTRY.registerData(StopAction.class, (c, cn) -> StopAction.INSTANCE);
    }

    @Override
    public void receivePacket(Connection connection, SerializableData packet) {
        super.receivePacket(connection, packet);
        DelegateEnvironment env = Environments.getEnvironment(connection);

        if (packet instanceof OnGroupMessageEvent groupMessageEvent)
            env.getMessageManager().onGroupMessage(groupMessageEvent.group, groupMessageEvent.user,
                    groupMessageEvent.message, groupMessageEvent.time);
        else if (packet instanceof OnFriendMessageEvent friendMessageEvent)
            env.getMessageManager().onFriendMessage(friendMessageEvent.user, friendMessageEvent.message,
                    friendMessageEvent.time);
        else if (packet instanceof OnStrangerMessageEvent strangerMessageEvent)
            env.getMessageManager().onStrangerMessage(strangerMessageEvent.user, strangerMessageEvent.message,
                    strangerMessageEvent.time);
        else if (packet instanceof OnGroupRecallEvent groupRecallEvent)
            env.getMessageManager().onGroupRecall(groupRecallEvent.group, groupRecallEvent.user, groupRecallEvent.time);
        else if (packet instanceof OnFriendRecallEvent friendRecallEvent)
            env.getMessageSender().onFriendRecall(friendRecallEvent.user, friendRecallEvent.time);
        else if (packet instanceof OnMemberAddEvent memberAddEvent)
            env.getMessageManager().onMemberAdd(memberAddEvent.group, memberAddEvent.user);
    }

    @Override
    public void connectionClosed(Connection connection) {
        super.connectionClosed(connection);
        Environments.removeEnvironment(Environments.getEnvironment(connection));
    }
}
