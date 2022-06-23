package io.github.nickid2018.koishibot.message.api;

import kotlin.Pair;
import kotlin.Triple;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface MessageEventPublisher {

    void subscribeGroupMessage(BiConsumer<Triple<GroupInfo, UserInfo, ChainMessage>, Long> consumer);

    void subscribeFriendMessage(BiConsumer<Pair<UserInfo, ChainMessage>, Long> consumer);

    void subscribeGroupTempMessage(BiConsumer<Pair<UserInfo, ChainMessage>, Long> consumer);

    void subscribeStrangerMessage(BiConsumer<Pair<UserInfo, ChainMessage>, Long> consumer);

    void subscribeNewMemberAdd(BiConsumer<GroupInfo, UserInfo> consumer);

    void subscribeGroupRecall(Consumer<Triple<GroupInfo, UserInfo, Long>> consumer);

    void subscribeFriendRecall(BiConsumer<UserInfo, Long> consumer);
}
