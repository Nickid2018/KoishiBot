package io.github.nickid2018.koishibot.message.kook;

import io.github.nickid2018.koishibot.message.api.*;
import io.github.nickid2018.koishibot.util.value.Either;
import io.github.zly2006.kookybot.contract.GuildUser;
import io.github.zly2006.kookybot.contract.User;
import io.github.zly2006.kookybot.message.ImageMessage;
import io.github.zly2006.kookybot.message.MarkdownMessage;
import io.github.zly2006.kookybot.message.Message;
import io.github.zly2006.kookybot.message.MessageComponent;

import java.util.regex.Pattern;

public class KOOKChain extends KOOKMessage implements ChainMessage {

    public static final Pattern AT_PATTERN = Pattern.compile("\\(met\\)\\w+?\\(met\\)");

    private Message[] message;

    public KOOKChain(KOOKEnvironment environment) {
        super(environment);
    }

    public KOOKChain(KOOKEnvironment environment, Message[] message) {
        super(environment);
        this.message = message;
    }

    @Override
    public void send(UserInfo contact) {
        for (Message send : message) {
            Either<User, GuildUser> user = ((KOOKUser) contact).getUser();
            if (user.isLeft())
                sentMessage = user.left().sendMessage(send);
            else
                sentMessage = user.right().sendMessage(send);
        }
    }

    @Override
    public void send(GroupInfo group) {
        for (Message send : message) {
            sentMessage = ((KOOKTextChannel) group).getChannel().sendMessage(send);
        }
    }

    @Override
    public ChainMessage fillChain(AbstractMessage... messages) {
        MarkdownMessage markdownMessage = new MarkdownMessage(environment.getKookClient(), "");
        Message imageMessage = null;
        for (AbstractMessage message : messages) {
            if (message instanceof TextMessage text)
                markdownMessage = new MarkdownMessage(
                        environment.getKookClient(), markdownMessage.content() + text.getText());
            else if (message instanceof KOOKAt at)
                markdownMessage.append(at.getKOOKMessage().right());
            else if (message instanceof KOOKImage image)
                imageMessage = image.getKOOKMessage().left();
        }
        if (imageMessage == null)
            message = new Message[] {markdownMessage};
        else
            message = new Message[] {markdownMessage, imageMessage};
        return this;
    }

    @Override
    public AbstractMessage[] getMessages() {
        if (message[0] instanceof ImageMessage image)
            return new AbstractMessage[] {new KOOKImage(environment, image)};
        if (message[0] instanceof MarkdownMessage markdownMessage)
            return new AbstractMessage[] {new KOOKText(environment, markdownMessage)};
        return new AbstractMessage[0];
    }

    @Override
    public Either<Message, MessageComponent> getKOOKMessage() {
        return Either.left(message[0]);
    }
}
