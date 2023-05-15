package io.github.nickid2018.koishibot.message;

import io.github.nickid2018.koishibot.core.ErrorRecord;
import io.github.nickid2018.koishibot.filter.AntiFilter;
import io.github.nickid2018.koishibot.filter.PostFilter;
import io.github.nickid2018.koishibot.filter.RequestFrequencyFilter;
import io.github.nickid2018.koishibot.filter.SensitiveFilter;
import io.github.nickid2018.koishibot.message.api.*;
import io.github.nickid2018.koishibot.util.Pair;
import io.github.nickid2018.koishibot.util.web.ErrorCodeException;
import io.github.nickid2018.koishibot.util.web.WebUtil;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

public class MessageSender {

    public static final String[] ERROR_MESSAGES = new String[] {
            "发生了错误", "bot发生了异常", "bot陷入无意识之中"
    };

    public static final int SEND_INTERVAL = 1000;

    private final Random random = new Random();
    private final ReentrantLock sendLock = new ReentrantLock();
    private final AtomicLong lastSentTime = new AtomicLong(System.currentTimeMillis());
    private final Queue<Pair<MessageContext, AbstractMessage>> sentQueue = new ConcurrentLinkedDeque<>();

    private final DelegateEnvironment environment;
    private final List<PostFilter> postFilters = new ArrayList<>();

    public MessageSender(DelegateEnvironment environment, boolean needAntiFilter) {
        this(environment, needAntiFilter, true);
    }

    public MessageSender(DelegateEnvironment environment, boolean needAntiFilter, boolean needSensitiveFilter) {
        this.environment = environment;

        if (needSensitiveFilter)
            postFilters.add(SensitiveFilter.INSTANCE);
        if (needAntiFilter)
            postFilters.add(new AntiFilter());
        postFilters.add(new RequestFrequencyFilter());
    }

    public void sendMessage(MessageContext context, AbstractMessage message) {
        send(context, message, false);
    }

    public void sendMessageRecallable(MessageContext context, AbstractMessage message) {
        send(context, message, true);
    }

    public void sendMessageAwait(MessageContext context, AbstractMessage message,
                                 BiConsumer<AbstractMessage, ChainMessage> consumer) {
        UserAwaitData.add(context.group(), context.user(), send(context, message, true), consumer);
    }

    public void sendMessageReply(MessageContext context, AbstractMessage message, boolean once,
                                 BiConsumer<AbstractMessage, ChainMessage> consumer) {
        MessageReplyData.add(context.group(), context.user(), send(context, message, false), consumer, once);
    }

    public void onError(Throwable t, String module, MessageContext context, boolean recall) {
        ErrorRecord.enqueueError(module, t);
        ChainMessage chain;
        String choose = ERROR_MESSAGES[random.nextInt(ERROR_MESSAGES.length)];
        if (t instanceof ErrorCodeException) {
            int code = ((ErrorCodeException) t).code;
            try {
                ImageMessage image = environment.newImage(new URL("https://http.cat/" + code));
                chain = environment.newChain(
                        environment.newQuote(context.message()),
                        environment.newText(choose + ": 状态码" + code),
                        image
                );
            } catch (IOException e) {
                chain = environment.newChain(
                        environment.newQuote(context.message()),
                        environment.newText(choose + ": 状态码" + code)
                );
            }
        } else {
            String message = t.getMessage();
            message = message == null ? "" : message;
            for (Map.Entry<String, String> url : WebUtil.MIRROR.entrySet()) {
                if (message.contains(url.getValue()))
                    message = message.replace(url.getValue(), url.getKey());
            }
            chain = environment.newChain(
                    environment.newQuote(context.message()),
                    environment.newText(choose + ": " + (message.length() > 100 ? t.getClass().getName() : message))
            );
        }
        send(context, chain, recall);
    }

    public AbstractMessage send(MessageContext context, AbstractMessage message, boolean recall) {
        for (PostFilter filter : postFilters)
            message = filter.filterMessagePost(message, context, environment);
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
                GroupInfo nowGroup = entry.first().group();
                UserInfo nowUser = entry.first().user();
                if (((groupInfo == null && nowGroup == null)
                        || (groupInfo != null && nowGroup != null && groupInfo.equals(nowGroup))) &&
                        user.equals(nowUser) && time == entry.first().sentTime())
                    messagesToRecall.add(entry);
            }
            messagesToRecall.forEach(en -> {
                sentQueue.remove(en);
                en.second().recall();
            });
        } catch (Exception e) {
            MessageManager.LOGGER.error("Error when recalling message", e);
        } finally {
            sendLock.unlock();
        }
    }

    public void onFriendRecall(UserInfo user, long time) {
        onRecall(null, user, time);
    }
}
