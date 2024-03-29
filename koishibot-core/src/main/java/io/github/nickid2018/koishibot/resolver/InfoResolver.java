package io.github.nickid2018.koishibot.resolver;

import io.github.nickid2018.koishibot.core.BotStart;
import io.github.nickid2018.koishibot.core.ErrorRecord;
import io.github.nickid2018.koishibot.message.DelegateEnvironment;
import io.github.nickid2018.koishibot.message.MessageResolver;
import io.github.nickid2018.koishibot.message.ResolverName;
import io.github.nickid2018.koishibot.message.Syntax;
import io.github.nickid2018.koishibot.message.api.AbstractMessage;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.permission.PermissionLevel;
import io.github.nickid2018.koishibot.util.AsyncUtil;

import java.util.Date;
import java.util.Locale;

@ResolverName("info")
@Syntax(syntax = "~info", help = "显示bot运行状态")
@Syntax(syntax = "~info error", help = "显示bot错误记录")
public class InfoResolver extends MessageResolver {

    public InfoResolver() {
        super("~info");
    }

    @Override
    public boolean needAt() {
        return true;
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, DelegateEnvironment environment) {
        key = key.trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty())
            printSystemData(context, environment);
        else {
            AbstractMessage message = switch (key) {
                case "error" -> getError(environment);
                default -> null;
            };
            environment.getMessageSender().sendMessage(context, message);
        }
        return true;
    }

    @Override
    public PermissionLevel getPermissionLevel() {
        return PermissionLevel.UNTRUSTED;
    }

    private void printSystemData(MessageContext context, DelegateEnvironment environment) {
        StringBuilder builder = new StringBuilder("Koishi bot 1.0 by Nickid2018\n");
        long time = System.currentTimeMillis() - BotStart.START_TIME;
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
        AsyncUtil.execute(
                () -> environment.getMessageSender().sendMessage(context, environment.newText(builder.toString())));
    }

    private AbstractMessage getError(Environment environment) {
        AbstractMessage message;
        if (environment.forwardMessageSupported())
            message = ErrorRecord.formatAsForwardMessage(environment);
        else
            message = environment.newText("环境不支持转发消息");
        if (message == null)
            message = environment.newText("无错误日志");
        return message;
    }
}
