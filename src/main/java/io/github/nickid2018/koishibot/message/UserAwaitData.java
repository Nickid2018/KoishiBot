package io.github.nickid2018.koishibot.message;

import io.github.nickid2018.koishibot.message.api.AbstractMessage;
import io.github.nickid2018.koishibot.message.api.ChainMessage;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import io.github.nickid2018.koishibot.util.AsyncUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

public class UserAwaitData {

    public static final Map<MessageData, BiConsumer<AbstractMessage, ChainMessage>> AWAIT_MAP = new HashMap<>();
    private static final ReentrantLock lock = new ReentrantLock();

    public static void add(GroupInfo group, UserInfo user, AbstractMessage sent, BiConsumer<AbstractMessage, ChainMessage> consumer) {
        lock.lock();
        AWAIT_MAP.put(new MessageData(group, user, sent), consumer);
        lock.unlock();
    }

    public static void onMessage(GroupInfo group, UserInfo user, ChainMessage reply) {
        MessageData find = null;
        lock.lock();
        for (MessageData data : AWAIT_MAP.keySet()) {
            if (group == null ^ data.group() == null)
                continue;
            if (group != null && !group.equals(data.group()))
                continue;
            if (user == null ^ data.user() == null)
                continue;
            if (user != null && !user.equals(data.user()))
                continue;
            find = data;
            break;
        }
        lock.unlock();
        if (find == null)
            return;
        BiConsumer<AbstractMessage, ChainMessage> dataConsumer;
        lock.lock();
        dataConsumer = AWAIT_MAP.remove(find);
        lock.unlock();
        MessageData finalFind = find;
        AsyncUtil.execute(() -> dataConsumer.accept(finalFind.sent(), reply));
    }
}

