package io.github.nickid2018.koishibot.core;

import kotlin.Pair;
import net.mamoe.mirai.contact.Friend;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.contact.Stranger;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.MessageReceipt;
import net.mamoe.mirai.message.data.*;
import net.mamoe.mirai.utils.ExternalResource;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

public class MessageInfo {

    public static final AtomicLong MESSAGE_COUNTER = new AtomicLong(0);
    public static final String[] ANTI_AUTO_FILTER = new String[]{
            "[ffk]", ">anti-auto_filter<", "~防止风向操控~", "=_禁止符卡攻击_="
    };
    public static final Random rand = new Random();

    public static final ReentrantLock SEND_LOCK = new ReentrantLock();
    public static final AtomicLong LAST_SEND_TIME = new AtomicLong(System.currentTimeMillis());

    public MessageChain data;

    public Member sender; // Group
    public Group group; // Nullable

    public Friend friend; // Nullable

    public Stranger stranger; // Nullable

    public MessageEvent event;

    public Message countAntiAutoFilter(Message message) {
        if (message instanceof ForwardMessage)
            return message;
        if (MESSAGE_COUNTER.getAndIncrement() % 10 == 0)
            return MessageUtils.newChain(
                    message,
                    new PlainText("\n" + ANTI_AUTO_FILTER[rand.nextInt(4)])
            );
        return message;
    }

    public void sendMessage(Message message) {
        message = countAntiAutoFilter(message);
        SEND_LOCK.lock();
        try {
            long sleepTime = 250 - (System.currentTimeMillis() - LAST_SEND_TIME.get());
            if (sleepTime > 0)
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ignored) {
                }
            if (group != null)
                group.sendMessage(message);
            if (friend != null)
                friend.sendMessage(message);
            if (stranger != null)
                stranger.sendMessage(message);
            if (group == null && friend == null && stranger == null && sender != null)
                sender.sendMessage(message);
            LAST_SEND_TIME.set(System.currentTimeMillis());
        } finally {
            SEND_LOCK.unlock();
        }
    }

    public void sendMessageWithQuote(Message message) {
        message = countAntiAutoFilter(message);
        SEND_LOCK.lock();
        try {
            long sleepTime = 250 - (System.currentTimeMillis() - LAST_SEND_TIME.get());
            if (sleepTime > 0)
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ignored) {
                }
            MessageReceipt<?> messageReceipt = sendAndGetReceipt(message);
            MessageManager.SENT_QUOTE_QUEUE.offer(new Pair<>(event, messageReceipt));
            while (!MessageManager.SENT_QUOTE_QUEUE.isEmpty() &&
                    event.getTime() - MessageManager.SENT_QUOTE_QUEUE.peek().component2().getSource().getTime() >= 1800_000)
                MessageManager.SENT_QUOTE_QUEUE.poll();
            LAST_SEND_TIME.set(System.currentTimeMillis());
        } finally {
            SEND_LOCK.unlock();
        }
    }

    public void sendMessageAwait(Message message, BiConsumer<MessageData, MessageInfo> consumer) {
        message = countAntiAutoFilter(message);
        SEND_LOCK.lock();
        try {
            long sleepTime = 250 - (System.currentTimeMillis() - LAST_SEND_TIME.get());
            if (sleepTime > 0)
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ignored) {
                }
            UserAwaitData.add(this, sendAndGetReceipt(message), consumer);
            LAST_SEND_TIME.set(System.currentTimeMillis());
        } finally {
            SEND_LOCK.unlock();
        }
    }

    private MessageReceipt<?> sendAndGetReceipt(Message message) {
        MessageReceipt<?> messageReceipt = null;
        if (group != null)
            messageReceipt = group.sendMessage(message);
        if (friend != null)
            messageReceipt = friend.sendMessage(message);
        if (stranger != null)
            messageReceipt = stranger.sendMessage(message);
        if (group == null && friend == null && stranger == null && sender != null)
            messageReceipt = sender.sendMessage(message);
        return  messageReceipt;
    }

    public Audio uploadAudio(ExternalResource resource) {
        if (group != null)
            return group.uploadAudio(resource);
        return null;
    }
}
