package io.github.nickid2018.koishibot.message;

import io.github.nickid2018.koishibot.core.ErrorRecord;
import io.github.nickid2018.koishibot.core.Settings;
import io.github.nickid2018.koishibot.filter.SensitiveWordFilter;
import io.github.nickid2018.koishibot.message.api.*;
import io.github.nickid2018.koishibot.message.api.ForwardMessage;
import io.github.nickid2018.koishibot.util.ErrorCodeException;
import io.github.nickid2018.koishibot.util.MutableBoolean;
import kotlin.Triple;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class MessageSender {

    public static final String[] ANTI_AUTO_FILTER = new String[]{
            "[ffk]", ">anti-auto_filter<", "~防止风向操控~", "=_禁止符卡攻击_="
    };
    public static final String[] ERROR_MESSAGES = new String[] {
            "发生了错误", "bot发生了异常", "bot陷入无意识之中"
    };

    private final Random random = new Random();
    private final AtomicLong messageCounter = new AtomicLong(0);
    private final ReentrantLock sendLock = new ReentrantLock();
    private final AtomicLong lastSentTime = new AtomicLong(System.currentTimeMillis());
    private final Queue<Triple<MessageContext, Long, AbstractMessage>> sentQueue = new ConcurrentLinkedDeque<>();

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
            return environment.newChain().fill(
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
            message = environment.newChain().fill(messages.toArray(new AbstractMessage[0]));
        }
        if (message instanceof TextMessage)
            message = environment.newText().fillText(
                    SensitiveWordFilter.filter(((TextMessage) message).getText(), filtered));
        if (filtered.getValue())
            message = environment.newChain().fill(
                    message,
                    environment.newText().fillText("\n<已经过关键词过滤>")
            );
        return message;
    }

    public void sendMessage(MessageContext contact, AbstractMessage message) {
        send(contact, message, false);
    }

    public void sendMessageRecallable(MessageContext contact, AbstractMessage message) {
        send(contact, message, true);
    }

    public void sendMessageAwait(MessageContext contact, AbstractMessage message, Consumer<ChainMessage> consumer) {
        send(contact, message, true);
        UserAwaitData.add(contact.getGroup(), contact.getUser(), consumer);
    }

    public void onError(Throwable t, String module, MessageContext context, boolean quote) {
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
        send(context, chain, quote);
        t.printStackTrace();
    }

    private void send(MessageContext contact, AbstractMessage message, boolean recall) {
        sendLock.lock();
        try {
            message = useSensitiveFilter(message);
            message = countAntiAutoFilter(message);
            long sleepTime = 250 - (System.currentTimeMillis() - lastSentTime.get());
            if (sleepTime > 0)
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ignored) {
                }
            contact.getSendDest().send(message);
            if (recall)
                sentQueue.offer(new Triple<>(contact, message.getSentTime(), message));
            while (sentQueue.size() > 100 ||
                    (!sentQueue.isEmpty() && System.currentTimeMillis() - sentQueue.peek().component2() >= 1800_000))
                sentQueue.poll();
            lastSentTime.set(System.currentTimeMillis());
        } finally {
            sendLock.unlock();
        }
    }

    public void onRecall(GroupInfo groupInfo, UserInfo user, long time) {
        sendLock.lock();
        try {
            for (Triple<MessageContext, Long, AbstractMessage> entry : sentQueue) {
                if (groupInfo.equals(entry.component1().getGroup()) &&
                        user.equals(entry.component1().getUser()) && time == entry.component2()) {
                    entry.component3().recall();
                    break;
                }
            }
        } finally {
            sendLock.unlock();
        }
    }

    public void onFriendRecall(UserInfo user, long time) {
        onRecall(null, user, time);
    }
}
