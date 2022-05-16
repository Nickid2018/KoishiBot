package io.github.nickid2018.koishibot.core;

import io.github.nickid2018.koishibot.Constants;
import io.github.nickid2018.koishibot.KoishiBotMain;
import kotlin.Triple;
import net.mamoe.mirai.message.data.ForwardMessage;
import net.mamoe.mirai.message.data.ForwardMessageBuilder;
import net.mamoe.mirai.message.data.PlainText;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

public class ErrorRecord {

    public static final Queue<Triple<Long, String, Throwable>> ERROR_QUEUE = new ConcurrentLinkedDeque<>();

    public static synchronized void enqueueError(String module, Throwable t) {
        ERROR_QUEUE.offer(new Triple<>(System.currentTimeMillis(), module, t));
        if (ERROR_QUEUE.size() > 30)
            ERROR_QUEUE.poll();
    }

    public static ForwardMessage formatAsForwardMessage() {
        List<Triple<Long, String, Throwable>> copied = new ArrayList<>(ERROR_QUEUE);
        if (copied.size() == 0)
            return null;
        ForwardMessageBuilder fmb = new ForwardMessageBuilder(
                Objects.requireNonNull(KoishiBotMain.INSTANCE.botKoishi.getFriend(2833231379L)));
        for (Triple<Long, String, Throwable> entry : copied) {
            StringBuilder builder = new StringBuilder();
            builder.append("错误时间: ").append(String.format("%tc", new Date(entry.getFirst()))).append("\n");
            builder.append("错误模块: ").append(entry.getSecond()).append("\n");
            builder.append("错误描述: ").append(entry.getThird().getMessage()).append("\n");
            builder.append("栈顶层: ").append(entry.getThird().getStackTrace()[0]);
            fmb.add(Settings.BOT_QQ, "Koishi bot", new PlainText(builder), Constants.TIME_OF_514);
        }
        return fmb.build();
    }
}
