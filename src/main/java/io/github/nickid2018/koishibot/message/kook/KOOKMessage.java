package io.github.nickid2018.koishibot.message.kook;

import io.github.kookybot.contract.GuildUser;
import io.github.kookybot.contract.User;
import io.github.kookybot.message.MarkdownMessage;
import io.github.kookybot.message.Message;
import io.github.kookybot.message.MessageComponent;
import io.github.kookybot.message.SelfMessage;
import io.github.nickid2018.koishibot.message.api.*;
import io.github.nickid2018.koishibot.util.value.Either;

public abstract class KOOKMessage implements AbstractMessage {

    protected final KOOKEnvironment environment;
    protected SelfMessage sentMessage;

    public KOOKMessage(KOOKEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public void send(UserInfo contact) {
        Either<Message, MessageComponent> message = getKOOKMessage();
        Message send = message.isRight() ?
                new MarkdownMessage(environment.getKookClient(), message.right().toMarkdown()) : message.left();
        Either<User, GuildUser> user = ((KOOKUser) contact).getUser();
        if (user.isLeft())
            sentMessage = user.left().sendMessage(send);
        else
            sentMessage = user.right().sendMessage(send);
    }

    @Override
    public void send(GroupInfo group) {
        Either<Message, MessageComponent> message = getKOOKMessage();
        Message send = message.isRight() ?
                new MarkdownMessage(environment.getKookClient(), message.right().toMarkdown()) : message.left();
        sentMessage = ((KOOKTextChannel) group).getChannel().sendMessage(send);
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
            return new KOOKMessageFrom(sentMessage.getId());
        }
        return null;
    }

    public abstract Either<Message, MessageComponent> getKOOKMessage();
}
