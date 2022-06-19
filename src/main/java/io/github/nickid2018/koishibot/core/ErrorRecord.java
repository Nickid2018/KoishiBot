package io.github.nickid2018.koishibot.core;

import io.github.nickid2018.koishibot.Constants;
import io.github.nickid2018.koishibot.message.api.ContactInfo;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.ForwardMessage;
import io.github.nickid2018.koishibot.message.api.MessageEntry;
import kotlin.Triple;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

public class ErrorRecord {

    public static final Queue<Triple<Long, String, Throwable>> ERROR_QUEUE = new ConcurrentLinkedDeque<>();

    public static synchronized void enqueueError(String module, Throwable t) {
        t.printStackTrace();
        ERROR_QUEUE.offer(new Triple<>(System.currentTimeMillis(), module, t));
        if (ERROR_QUEUE.size() > 30)
            ERROR_QUEUE.poll();
    }

    public static ForwardMessage formatAsForwardMessage(Environment environment) {
        List<Triple<Long, String, Throwable>> copied = new ArrayList<>(ERROR_QUEUE);
        if (copied.size() == 0)
            return null;
        ForwardMessage forwards = environment.newForwards();
        ContactInfo contact = environment.getUser(environment.getBotId(), false);
        List<MessageEntry> entries = new ArrayList<>();
        for (Triple<Long, String, Throwable> entry : copied) {
            StringBuilder builder = new StringBuilder();
            builder.append("错误时间: ").append(String.format("%tc", new Date(entry.getFirst()))).append("\n");
            builder.append("错误模块: ").append(entry.getSecond()).append("\n");
            builder.append("错误描述: ").append(entry.getThird().getMessage()).append("\n");
            builder.append("栈顶层: ").append(entry.getThird().getStackTrace()[0]);
            entries.add(environment.newMessageEntry(
                    environment.getBotId(),
                    "Koishi bot",
                    environment.newText(builder.toString().trim()),
                    Constants.TIME_OF_514
            ));
        }
        forwards.fill(contact, entries.toArray(new MessageEntry[0]));
        return forwards;
    }
}
