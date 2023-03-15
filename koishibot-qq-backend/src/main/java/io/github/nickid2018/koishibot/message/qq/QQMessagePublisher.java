package io.github.nickid2018.koishibot.message.qq;

import io.github.nickid2018.koishibot.backend.Main;
import io.github.nickid2018.koishibot.message.event.*;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import net.mamoe.mirai.event.EventChannel;
import net.mamoe.mirai.event.ListeningStatus;
import net.mamoe.mirai.event.events.*;

public class QQMessagePublisher {

    private final QQEnvironment environment;
    private final EventChannel<BotEvent> eventChannel;

    public QQMessagePublisher(QQEnvironment environment) {
        this.environment = environment;
        eventChannel = environment.getBot().getEventChannel();
        subscribeGroupMessage();
        subscribeFriendMessage();
        subscribeGroupTempMessage();
        subscribeStrangerMessage();
        subscribeNewMemberAdd();
        subscribeGroupRecall();
        subscribeFriendRecall();
    }

    public void subscribeGroupMessage() {
        eventChannel.exceptionHandler(createHandler("qq.group.message"))
                .subscribe(GroupMessageEvent.class, messageEvent -> {
                    if (messageEvent.getSender().getId() == 80000000)
                        return ListeningStatus.LISTENING;
                    OnGroupMessageEvent event = new OnGroupMessageEvent(environment);
                    event.group = new QQGroup(environment, messageEvent.getGroup());
                    event.user = new QQUser(environment, messageEvent.getSender(), true);
                    event.message = new QQChain(environment, messageEvent.getMessage());
                    event.time = messageEvent.getTime();
                    environment.getConnection().sendPacket(event);
                    return ListeningStatus.LISTENING;
                });
    }

    public void subscribeFriendMessage() {
        eventChannel.exceptionHandler(createHandler("qq.friend.message"))
                .subscribe(FriendMessageEvent.class, messageEvent -> {
                    OnFriendMessageEvent event = new OnFriendMessageEvent(environment);
                    event.user = new QQUser(environment, messageEvent.getSender(), false);
                    event.message = new QQChain(environment, messageEvent.getMessage());
                    event.time = messageEvent.getTime();
                    environment.getConnection().sendPacket(event);
                    return ListeningStatus.LISTENING;
                });
    }

    public void subscribeGroupTempMessage() {
        eventChannel.exceptionHandler(createHandler("qq.temp.message"))
                .subscribe(GroupTempMessageEvent.class, messageEvent -> {
                    OnStrangerMessageEvent event = new OnStrangerMessageEvent(environment);
                    event.user = new QQUser(environment, messageEvent.getSender(), true);
                    event.message = new QQChain(environment, messageEvent.getMessage());
                    event.time = messageEvent.getTime();
                    environment.getConnection().sendPacket(event);
                    return ListeningStatus.LISTENING;
                });
    }

    public void subscribeStrangerMessage() {
        eventChannel.exceptionHandler(createHandler("qq.stranger.message"))
                .subscribe(StrangerMessageEvent.class, messageEvent -> {
                    OnStrangerMessageEvent event = new OnStrangerMessageEvent(environment);
                    event.user = new QQUser(environment, messageEvent.getSender(), true);
                    event.message = new QQChain(environment, messageEvent.getMessage());
                    event.time = messageEvent.getTime();
                    environment.getConnection().sendPacket(event);
                    return ListeningStatus.LISTENING;
                });
    }

    public void subscribeNewMemberAdd() {
        eventChannel.exceptionHandler(createHandler("qq.group.join"))
                .subscribe(MemberJoinEvent.class, messageEvent -> {
                    OnMemberAddEvent event = new OnMemberAddEvent(environment);
                    event.group = new QQGroup(environment, messageEvent.getGroup());
                    event.user = new QQUser(environment, messageEvent.getUser(), true);
                    environment.getConnection().sendPacket(event);
                    return ListeningStatus.LISTENING;
                });
    }

    public void subscribeGroupRecall() {
        eventChannel.exceptionHandler(createHandler("qq.group.recall"))
                .subscribe(MessageRecallEvent.GroupRecall.class, messageEvent -> {
                    OnGroupRecallEvent event = new OnGroupRecallEvent(environment);
                    event.group = new QQGroup(environment, messageEvent.getGroup());
                    event.user = new QQUser(environment, messageEvent.getAuthor(), true);
                    event.time = messageEvent.getMessageTime();
                    environment.getConnection().sendPacket(event);
                    return ListeningStatus.LISTENING;
                });
    }

    public void subscribeFriendRecall() {
        eventChannel.exceptionHandler(createHandler("qq.friend.recall"))
                .subscribe(MessageRecallEvent.FriendRecall.class, messageEvent -> {
                    OnFriendRecallEvent event = new OnFriendRecallEvent(environment);
                    event.user = new QQUser(environment, messageEvent.getAuthor(), true);
                    event.time = messageEvent.getMessageTime();
                    environment.getConnection().sendPacket(event);
                    return ListeningStatus.LISTENING;
                });
    }

    private static Function1<Throwable, Unit> createHandler(String name) {
        return exception -> {
            Main.LOGGER.error("Error occurred when handling " + name + " event.", exception);
            return Unit.INSTANCE;
        };
    }
}
