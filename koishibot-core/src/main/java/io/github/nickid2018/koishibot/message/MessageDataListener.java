package io.github.nickid2018.koishibot.message;

import io.github.nickid2018.koishibot.core.BotStart;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.event.*;
import io.github.nickid2018.koishibot.message.network.DataPacketListener;
import io.github.nickid2018.koishibot.network.Connection;
import io.github.nickid2018.koishibot.network.SerializableData;

public class MessageDataListener extends DataPacketListener {

    public MessageDataListener() {
        super((c, cn) -> {
            try {
                if (c.equals(Environment.class))
                    return new DelegateEnvironment(cn);
                Environment environment = Environments.getEnvironment(cn);
                if (environment == null)
                    return null;
                BotStart.LOGGER.debug("Creating data in environment {}: {}", environment.getEnvironmentName(), c.getName());
                return c.getConstructor(Environment.class).newInstance(environment);
            } catch (Exception e) {
                return null;
            }
        });
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
