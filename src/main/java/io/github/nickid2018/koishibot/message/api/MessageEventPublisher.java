package io.github.nickid2018.koishibot.message.api;

import kotlin.Triple;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface MessageEventPublisher {

    void subscribeGroupMessage(Consumer<Triple<GroupInfo, UserInfo, ChainMessage>> consumer);

    void subscribeFriendMessage(BiConsumer<UserInfo, ChainMessage> consumer);

    void subscribeGroupTempMessage(BiConsumer<UserInfo, ChainMessage> consumer);

    void subscribeStrangerMessage(BiConsumer<UserInfo, ChainMessage> consumer);

    void subscribeNewMemberAdd(BiConsumer<GroupInfo, UserInfo> consumer);

    void subscribeGroupRecall(Consumer<Triple<GroupInfo, UserInfo, Long>> consumer);

    void subscribeFriendRecall(BiConsumer<UserInfo, Long> consumer);
}
