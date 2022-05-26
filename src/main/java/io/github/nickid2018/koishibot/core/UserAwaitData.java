package io.github.nickid2018.koishibot.core;

import io.github.nickid2018.koishibot.KoishiBotMain;
import net.mamoe.mirai.message.MessageReceipt;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

public class UserAwaitData {

    public static final Map<MessageData, BiConsumer<MessageData, MessageInfo>> AWAIT_MAP = new HashMap<>();
    private static final ReentrantLock lock = new ReentrantLock();

    public static void add(MessageInfo info, MessageReceipt<?> receipt, BiConsumer<MessageData, MessageInfo> consumer) {
        lock.lock();
        AWAIT_MAP.put(new MessageData(info, receipt), consumer);
        lock.unlock();
    }

    public static void onMessage(MessageInfo info) {
        MessageData find = null;
        lock.lock();
        for (MessageData data : AWAIT_MAP.keySet()) {
            if (data.equals(info)) {
                find = data;
                break;
            }
        }
        lock.unlock();
        if (find == null)
            return;
        BiConsumer<MessageData, MessageInfo> dataConsumer;
        lock.lock();
        dataConsumer = AWAIT_MAP.remove(find);
        lock.unlock();
        MessageData finalFind = find;
        KoishiBotMain.INSTANCE.executor.execute(() -> dataConsumer.accept(finalFind, info));
    }
}
