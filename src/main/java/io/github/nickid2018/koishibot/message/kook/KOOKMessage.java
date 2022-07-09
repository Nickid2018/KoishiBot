package io.github.nickid2018.koishibot.message.kook;

import io.github.nickid2018.koishibot.message.api.*;
import io.github.zly2006.kookybot.contract.TextChannel;
import io.github.zly2006.kookybot.message.Message;
import io.github.zly2006.kookybot.message.SelfMessage;

public abstract class KOOKMessage implements AbstractMessage {

    private final KOOKEnvironment environment;

    private SelfMessage sentMessage;

    public KOOKMessage(KOOKEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public void send(UserInfo contact) {
        sentMessage = ((KOOKUser) contact).getUser().sendMessage(getKOOKMessage());
    }

    @Override
    public void send(GroupInfo group) {
        sentMessage = ((KOOKTextChannel) group).getChannel().sendMessage(getKOOKMessage());
    }

    @Override
    public void recall() {
        if (sentMessage != null)
            sentMessage.delete();
    }

    @Override
    public long getSentTime() {
        return sentMessage.getTimestamp();
    }

    @Override
    public MessageFrom getSource() {
        if (sentMessage != null) {
            return new KOOKMessageFrom(sentMessage.getTarget() instanceof TextChannel channel ? channel : null,
                    environment.getKookClient().getUser(environment.getSelf().getId()), getSentTime());
        }
        return null;
    }

    public abstract Message getKOOKMessage();
}
