package io.github.nickid2018.koishibot.message.kook;

import io.github.kookybot.message.Message;
import io.github.kookybot.message.MessageComponent;
import io.github.nickid2018.koishibot.message.api.AbstractMessage;
import io.github.nickid2018.koishibot.message.api.ChainMessage;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import io.github.nickid2018.koishibot.util.value.Either;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class KOOKChain extends KOOKMessage implements ChainMessage {

    public static final Pattern AT_PATTERN = Pattern.compile("\\(met\\)\\w+?\\(met\\)");

    private Either<Message, MessageComponent>[] message;
    private KOOKMessageSource msgSource;

    public KOOKChain(KOOKEnvironment environment) {
        super(environment);
    }

    public KOOKChain(KOOKEnvironment environment, Either<Message, MessageComponent>[] message, KOOKMessageSource msgSource) {
        super(environment);
        this.message = message;
        this.msgSource = msgSource;
    }

    @Override
    public void send(UserInfo contact) {
//        for (Message send : message) {
//            Either<User, GuildUser> user = ((KOOKUser) contact).getUser();
//            if (user.isLeft())
//                sentMessage = user.left().sendMessage(send);
//            else
//                sentMessage = user.right().sendMessage(send);
//        }
    }

    @Override
    public void send(GroupInfo group) {
//        for (Message send : message) {
//            sentMessage = ((KOOKTextChannel) group).getChannel().sendMessage(send);
//        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public ChainMessage fillChain(AbstractMessage... messages) {
        List<Either<Message, MessageComponent>> list = Stream.of(messages)
                .filter(s -> s instanceof KOOKMessage && !(s instanceof KOOKChain))
                .map(s -> (KOOKMessage) s).map(KOOKMessage::getKOOKMessage).collect(Collectors.toList());
        Stream.of(messages).filter(s -> s instanceof KOOKChain)
                .map(s -> (KOOKChain) s).map(s -> s.message)
                .forEach(s -> list.addAll(List.of(s)));
        message = list.toArray(Either[]::new);
        return this;
    }

    @Override
    public AbstractMessage[] getMessages() {

        return new AbstractMessage[0];
    }

    @Override
    public Either<Message, MessageComponent> getKOOKMessage() {
        return null;
    }

    public KOOKMessageSource getMsgSource() {
        return msgSource;
    }
}
