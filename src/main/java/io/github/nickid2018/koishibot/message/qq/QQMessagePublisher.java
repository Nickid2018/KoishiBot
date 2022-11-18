package io.github.nickid2018.koishibot.message.qq;

import io.github.nickid2018.koishibot.core.ErrorRecord;
import io.github.nickid2018.koishibot.message.api.*;
import kotlin.Pair;
import kotlin.Triple;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import net.mamoe.mirai.event.EventChannel;
import net.mamoe.mirai.event.ListeningStatus;
import net.mamoe.mirai.event.events.*;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class QQMessagePublisher implements MessageEventPublisher {

    private final QQEnvironment environment;
    private final EventChannel<BotEvent> eventChannel;

    public QQMessagePublisher(QQEnvironment environment) {
        this.environment = environment;
        eventChannel = environment.getBot().getEventChannel();
    }

    @Override
    public void subscribeGroupMessage(BiConsumer<Triple<GroupInfo, UserInfo, ChainMessage>, Long> consumer) {
        eventChannel.exceptionHandler(createHandler("qq.group.message"))
                .subscribe(GroupMessageEvent.class, messageEvent -> {
                    if (messageEvent.getSender().getId() == 80000000)
                        return ListeningStatus.LISTENING;
                    GroupInfo group = new QQGroup(environment, messageEvent.getGroup());
                    UserInfo user = new QQUser(environment, messageEvent.getSender(), false, true);
                    ChainMessage message = new QQChain(environment, messageEvent.getMessage());
                    consumer.accept(new Triple<>(group, user, message), (long) messageEvent.getTime());
                    return ListeningStatus.LISTENING;
                });
    }

    @Override
    public void subscribeFriendMessage(BiConsumer<Pair<UserInfo, ChainMessage>, Long> consumer) {
        eventChannel.exceptionHandler(createHandler("qq.friend.message"))
                .subscribe(FriendMessageEvent.class, messageEvent -> {
                    UserInfo user = new QQUser(environment, messageEvent.getSender(), false, false);
                    ChainMessage message = new QQChain(environment, messageEvent.getMessage());
                    consumer.accept(new Pair<>(user, message), (long) messageEvent.getTime());
                    return ListeningStatus.LISTENING;
                });
    }

    @Override
    public void subscribeGroupTempMessage(BiConsumer<Pair<UserInfo, ChainMessage>, Long> consumer) {
        eventChannel.exceptionHandler(createHandler("qq.temp.message"))
                .subscribe(GroupTempMessageEvent.class, messageEvent -> {
                    UserInfo user = new QQUser(environment, messageEvent.getSender(), true, true);
                    ChainMessage message = new QQChain(environment, messageEvent.getMessage());
                    consumer.accept(new Pair<>(user, message), (long) messageEvent.getTime());
                    return ListeningStatus.LISTENING;
                });
    }

    @Override
    public void subscribeStrangerMessage(BiConsumer<Pair<UserInfo, ChainMessage>, Long> consumer) {
        eventChannel.exceptionHandler(createHandler("qq.stranger.message"))
                .subscribe(StrangerMessageEvent.class, messageEvent -> {
                    UserInfo user = new QQUser(environment, messageEvent.getSender(), true, false);
                    ChainMessage message = new QQChain(environment, messageEvent.getMessage());
                    consumer.accept(new Pair<>(user, message), (long) messageEvent.getTime());
                    return ListeningStatus.LISTENING;
                });
    }

    @Override
    public void subscribeNewMemberAdd(BiConsumer<GroupInfo, UserInfo> consumer) {
        eventChannel.exceptionHandler(createHandler("qq.group.join"))
                .subscribe(MemberJoinEvent.class, messageEvent -> {
                    GroupInfo group = new QQGroup(environment, messageEvent.getGroup());
                    UserInfo user = new QQUser(environment, messageEvent.getUser(), false, true);
                    consumer.accept(group, user);
                    return ListeningStatus.LISTENING;
                });
    }

    @Override
    public void subscribeGroupRecall(Consumer<Triple<GroupInfo, UserInfo, Long>> consumer) {
        eventChannel.exceptionHandler(createHandler("qq.group.recall"))
                .subscribe(MessageRecallEvent.GroupRecall.class, messageEvent -> {
                    GroupInfo group = new QQGroup(environment, messageEvent.getGroup());
                    UserInfo user = new QQUser(environment, messageEvent.component6(), false, true);
                    consumer.accept(new Triple<>(group, user, (long) messageEvent.getMessageTime()));
                    return ListeningStatus.LISTENING;
                });
    }

    @Override
    public void subscribeFriendRecall(BiConsumer<UserInfo, Long> consumer) {
        eventChannel.exceptionHandler(createHandler("qq.friend.recall"))
                .subscribe(MessageRecallEvent.FriendRecall.class, messageEvent -> {
                    UserInfo user = new QQUser(environment, messageEvent.component6(), false, true);
                    consumer.accept(user, (long) messageEvent.getMessageTime());
                    return ListeningStatus.LISTENING;
                });
    }

    private static Function1<Throwable, Unit> createHandler(String name) {
        return exception -> {
            ErrorRecord.enqueueError(name, exception);
            return Unit.INSTANCE;
        };
    }
}
