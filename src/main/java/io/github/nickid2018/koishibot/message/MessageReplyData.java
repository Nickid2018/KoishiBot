package io.github.nickid2018.koishibot.message;

import io.github.nickid2018.koishibot.message.api.AbstractMessage;
import io.github.nickid2018.koishibot.message.api.ChainMessage;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import io.github.nickid2018.koishibot.util.AsyncUtil;
import kotlin.Pair;

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

    public static void onMessage(GroupInfo group, UserInfo user, ChainMessage reply) {
        Pair<MessageData, Boolean> find = null;
        lock.lock();
        for (Pair<MessageData, Boolean> data : REPLIES.keySet()) {
            if (group == null ^ data.getFirst().group == null)
                continue;
            if (group != null && !group.equals(data.getFirst().group))
                continue;
            if (group == null && !user.equals(data.getFirst().user))
                continue;
            find = data;
            break;
        }
        lock.unlock();
        if (find == null)
            return;
        BiConsumer<AbstractMessage, ChainMessage> dataConsumer;
        lock.lock();
        dataConsumer = find.getSecond() ? REPLIES.remove(find) :  REPLIES.get(find);
        lock.unlock();
        MessageData finalFind = find.getFirst();
        AsyncUtil.execute(() -> dataConsumer.accept(finalFind.sent, reply));
    }
}
