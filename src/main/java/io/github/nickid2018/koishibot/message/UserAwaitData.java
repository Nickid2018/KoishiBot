package io.github.nickid2018.koishibot.message;

import io.github.nickid2018.koishibot.KoishiBotMain;
import io.github.nickid2018.koishibot.message.api.ChainMessage;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.UserInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class UserAwaitData {

    private static class MessageData {

        public GroupInfo group;
        public UserInfo user;

        public MessageData(GroupInfo group, UserInfo user) {
            this.group = group;
            this.user = user;
        }
    }

    public static final Map<MessageData, Consumer<ChainMessage>> AWAIT_MAP = new HashMap<>();
    private static final ReentrantLock lock = new ReentrantLock();

    public static void add(GroupInfo group, UserInfo user, Consumer<ChainMessage> consumer) {
        lock.lock();
        AWAIT_MAP.put(new MessageData(group, user), consumer);
        lock.unlock();
    }

    public static void onMessage(GroupInfo group, UserInfo user, ChainMessage reply) {
        MessageData find = null;
        lock.lock();
        for (MessageData data : AWAIT_MAP.keySet()) {
            if (group == null ^ data.group == null)
                continue;
            if (group != null && !group.equals(data.group))
                continue;
            if (user == null ^ data.user == null)
                continue;
            if (user != null && !user.equals(data.user))
                continue;
            find = data;
            break;
        }
        lock.unlock();
        if (find == null)
            return;
        Consumer<ChainMessage> dataConsumer;
        lock.lock();
        dataConsumer = AWAIT_MAP.remove(find);
        lock.unlock();
        KoishiBotMain.INSTANCE.executor.execute(() -> dataConsumer.accept(reply));
    }
}

