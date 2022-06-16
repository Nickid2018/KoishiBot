package io.github.nickid2018.koishibot.resolver;

import io.github.nickid2018.koishibot.core.ErrorRecord;
import io.github.nickid2018.koishibot.KoishiBotMain;
import io.github.nickid2018.koishibot.core.MessageInfo;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.PlainText;

import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

public class InfoResolver extends MessageResolver {

    public InfoResolver() {
        super("~info");
    }

    @Override
    public boolean needAt() {
        return true;
    }

    @Override
    public boolean resolveInternal(String key, MessageInfo info, Pattern pattern) {
        key = key.trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty())
            printSystemData(info);
        else {
            Message message = null;
            switch (key) {
                case "error":
                    message = getError();
                    break;
            }
            info.sendMessage(message);
        }
        return true;
    }

    private void printSystemData(MessageInfo info) {
        StringBuilder builder = new StringBuilder("Koishi bot 1.0-SNAPSHOT by Nickid2018\n");
        long time = System.currentTimeMillis() - KoishiBotMain.INSTANCE.startTime;
        builder.append("已运行时间：").append(time / 86400_000).append("天").append(time % 86400_000 / 3600_000).append("小时")
                        .append(time % 3600_000 / 60_000).append("分").append(time % 60_000 / 1_000).append("秒")
                        .append(time % 1_000).append("毫秒\n");
        builder.append("现在时间: ").append(String.format("%tc", new Date())).append("\n");
        builder.append("内存状况: ");
        double max = Runtime.getRuntime().maxMemory();
        double total = Runtime.getRuntime().totalMemory();
        double free = Runtime.getRuntime().freeMemory();
        builder.append("使用").append(String.format("%.2f", (total - free) / 1048576)).append("M, ");
        builder.append("最大堆内存").append(String.format("%.2f", max / 1048576)).append("M, ");
        builder.append("占比").append(String.format("%.2f", (total - free) / max * 100)).append("%\n");
        builder.append("系统信息: ").append(System.getProperty("os.name")).append("\n");
        builder.append("项目已在Github上开源: https://github.com/Nickid2018/KoishiBot (AGPL v3)");
        KoishiBotMain.INSTANCE.executor.execute(() -> info.sendMessage(new PlainText(builder)));
    }

    private Message getError() {
        Message message = ErrorRecord.formatAsForwardMessage();
        if (message == null)
            message = new PlainText("无错误日志");
        return message;
    }
}
