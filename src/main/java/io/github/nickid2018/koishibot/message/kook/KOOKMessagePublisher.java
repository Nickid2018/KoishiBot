package io.github.nickid2018.koishibot.message.kook;

import io.github.nickid2018.koishibot.message.api.ChainMessage;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.MessageEventPublisher;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import kotlin.Pair;
import kotlin.Triple;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class KOOKMessagePublisher implements MessageEventPublisher {



    @Override
    public void subscribeGroupMessage(BiConsumer<Triple<GroupInfo, UserInfo, ChainMessage>, Long> consumer) {

    }

    @Override
    public void subscribeFriendMessage(BiConsumer<Pair<UserInfo, ChainMessage>, Long> consumer) {

    }

    @Override
    public void subscribeGroupTempMessage(BiConsumer<Pair<UserInfo, ChainMessage>, Long> consumer) {

    }

    @Override
    public void subscribeStrangerMessage(BiConsumer<Pair<UserInfo, ChainMessage>, Long> consumer) {

    }

    @Override
    public void subscribeNewMemberAdd(BiConsumer<GroupInfo, UserInfo> consumer) {

    }

    @Override
    public void subscribeGroupRecall(Consumer<Triple<GroupInfo, UserInfo, Long>> consumer) {

    }

    @Override
    public void subscribeFriendRecall(BiConsumer<UserInfo, Long> consumer) {

    }
}
