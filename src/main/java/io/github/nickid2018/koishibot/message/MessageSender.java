package io.github.nickid2018.koishibot.message;

import io.github.nickid2018.koishibot.core.ErrorRecord;
import io.github.nickid2018.koishibot.core.Settings;
import io.github.nickid2018.koishibot.filter.SensitiveWordFilter;
import io.github.nickid2018.koishibot.message.api.*;
import io.github.nickid2018.koishibot.util.ErrorCodeException;
import io.github.nickid2018.koishibot.util.value.MutableBoolean;
import kotlin.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

public class MessageSender {

    public static final String[] ANTI_AUTO_FILTER = new String[]{
            "[ffk]", ">anti-auto_filter<", "~防止风向操控~", "=_禁止符卡攻击_="
    };

    public static final String[] ERROR_MESSAGES = new String[] {
            "发生了错误", "bot发生了异常", "bot陷入无意识之中"
    };

    public static final int SEND_INTERVAL = 1000;

    private final Random random = new Random();
    private final AtomicLong messageCounter = new AtomicLong(0);
    private final ReentrantLock sendLock = new ReentrantLock();
    private final AtomicLong lastSentTime = new AtomicLong(System.currentTimeMillis());
    private final Queue<Pair<MessageContext, AbstractMessage>> sentQueue = new ConcurrentLinkedDeque<>();

    private final Environment environment;
    private final boolean needAntiFilter;

    public MessageSender(Environment environment, boolean needAntiFilter) {
        this.environment = environment;
        this.needAntiFilter = needAntiFilter;
    }

    private AbstractMessage countAntiAutoFilter(AbstractMessage message) {
        if (!needAntiFilter)
            return message;
        if (message instanceof ForwardMessage || message instanceof AudioMessage || message instanceof ImageMessage)
            return message;
        if (messageCounter.getAndIncrement() % 10 == 0)
            return environment.newChain().fillChain(
                    message,
                    environment.newText().fillText("\n" + ANTI_AUTO_FILTER[random.nextInt(ANTI_AUTO_FILTER.length)])
            );
        return message;
    }

    private AbstractMessage useSensitiveFilter(AbstractMessage message) {
        MutableBoolean filtered = new MutableBoolean(false);
        if (message instanceof ChainMessage) {
            List<AbstractMessage> messages = new ArrayList<>();
            for (AbstractMessage mess : ((ChainMessage) message).getMessages()) {
                if (mess instanceof TextMessage)
                    mess = environment.newText().fillText(
                            SensitiveWordFilter.filter(((TextMessage) mess).getText(), filtered));
                messages.add(mess);
            }
            message = environment.newChain().fillChain(messages.toArray(new AbstractMessage[0]));
        }
        if (message instanceof TextMessage)
            message = environment.newText().fillText(
                    SensitiveWordFilter.filter(((TextMessage) message).getText(), filtered));
        if (filtered.getValue())
            message = environment.newChain().fillChain(
                    message,
                    environment.newText().fillText("\n<已经过关键词过滤>")
            );
        return message;
    }

    public void sendMessage(MessageContext context, AbstractMessage message) {
        send(context, message, false);
    }

    public void sendMessageRecallable(MessageContext context, AbstractMessage message) {
        send(context, message, true);
    }

    public void sendMessageAwait(MessageContext context, AbstractMessage message, BiConsumer<AbstractMessage, ChainMessage> consumer) {
        UserAwaitData.add(context.getGroup(), context.getUser(), send(context, message, true), consumer);
    }

    public void sendMessageReply(MessageContext context, AbstractMessage message, boolean once,
                                 BiConsumer<AbstractMessage, ChainMessage> consumer) {
        MessageReplyData.add(context.getGroup(), context.getUser(), send(context, message, false), consumer, once);
    }

    public void onError(Throwable t, String module, MessageContext context, boolean recall) {
        ErrorRecord.enqueueError(module, t);
        ChainMessage chain;
        String choose = ERROR_MESSAGES[random.nextInt(ERROR_MESSAGES.length)];
        if (t instanceof ErrorCodeException) {
            int code = ((ErrorCodeException) t).code;
            try {
                ImageMessage image = environment.newImage();
                try (InputStream is = new URL("https://http.cat/" + code).openStream()) {
                    image.fillImage(is);
                }
                chain = environment.newChain(
                        environment.newQuote(context.getMessage()),
                        environment.newText(choose + ": 状态码" + code),
                        image
                );
            } catch (IOException e) {
                chain = environment.newChain(
                        environment.newQuote(context.getMessage()),
                        environment.newText(choose + ": 状态码" + code)
                );
            }
        } else {
            String message = t.getMessage();
            for (Map.Entry<String, String> url : Settings.MIRROR.entrySet()) {
                if (message.contains(url.getValue()))
                    message = message.replace(url.getValue(), url.getKey());
            }
            chain = environment.newChain(
                    environment.newQuote(context.getMessage()),
                    environment.newText(choose + ": " + (message.length() > 100 ? t.getClass().getName() : message))
            );
        }
        send(context, chain, recall);
    }

    public AbstractMessage send(MessageContext context, AbstractMessage message, boolean recall) {
        message = useSensitiveFilter(message);
        message = countAntiAutoFilter(message);
        sendLock.lock();
        try {
            long sleepTime = SEND_INTERVAL - (System.currentTimeMillis() - lastSentTime.get());
            if (sleepTime > 0)
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ignored) {
                }
            context.getSendDest().send(message);
            if (recall)
                sentQueue.offer(new Pair<>(context, message));
            while (sentQueue.size() > 100)
                sentQueue.poll();
            lastSentTime.set(System.currentTimeMillis());
        } finally {
            sendLock.unlock();
        }
        return message;
    }

    public void onRecall(GroupInfo groupInfo, UserInfo user, long time) {
        sendLock.lock();
        try {
            List<Pair<MessageContext, AbstractMessage>> messagesToRecall = new ArrayList<>();
            for (Pair<MessageContext, AbstractMessage> entry : sentQueue) {
                GroupInfo nowGroup = entry.component1().getGroup();
                UserInfo nowUser = entry.component1().getUser();
                if (((groupInfo == null && nowGroup == null)
                        || (groupInfo != null && nowGroup != null && groupInfo.equals(nowGroup))) &&
                        user.equals(nowUser) && time == entry.component1().getSentTime())
                    messagesToRecall.add(entry);
            }
            messagesToRecall.forEach(en -> {
                sentQueue.remove(en);
                en.component2().recall();
            });
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            sendLock.unlock();
        }
    }

    public void onFriendRecall(UserInfo user, long time) {
        onRecall(null, user, time);
    }
}
