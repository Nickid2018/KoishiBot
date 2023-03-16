package io.github.nickid2018.koishibot.message;

import io.github.nickid2018.koishibot.message.api.*;
import io.github.nickid2018.koishibot.util.AsyncUtil;
import io.github.nickid2018.koishibot.util.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

public class MessageReplyData {

    private static final Map<Pair<MessageData, Boolean>, BiConsumer<AbstractMessage, ChainMessage>> REPLIES = new HashMap<>();
    private static final ReentrantLock lock = new ReentrantLock();

    public static void add(GroupInfo group, UserInfo user, AbstractMessage sent,
                           BiConsumer<AbstractMessage, ChainMessage> consumer, boolean once) {
        lock.lock();
        REPLIES.put(new Pair<>(new MessageData(group, user, sent), once), consumer);
        lock.unlock();
    }

    public static void onMessage(GroupInfo group, UserInfo user, QuoteMessage reply, ChainMessage chain) {
        Pair<MessageData, Boolean> find = null;
        lock.lock();
        for (Pair<MessageData, Boolean> data : REPLIES.keySet()) {
            if (group == null ^ data.first().group() == null)
                continue;
            if (group != null && !group.equals(data.first().group()))
                continue;
            if (group == null && !user.equals(data.first().user()))
                continue;
            if (!data.first().sent().getSource().equals(reply.getQuoteFrom()))
                continue;
            find = data;
            break;
        }
        lock.unlock();
        if (find == null)
            return;
        BiConsumer<AbstractMessage, ChainMessage> dataConsumer;
        lock.lock();
        dataConsumer = find.second() ? REPLIES.remove(find) :  REPLIES.get(find);
        lock.unlock();
        MessageData finalFind = find.first();
        AsyncUtil.execute(() -> dataConsumer.accept(finalFind.sent(), chain));
    }
}
