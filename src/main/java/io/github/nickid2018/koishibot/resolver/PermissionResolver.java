package io.github.nickid2018.koishibot.resolver;

import io.github.nickid2018.koishibot.message.api.AtMessage;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.permission.PermissionLevel;
import io.github.nickid2018.koishibot.permission.PermissionManager;
import io.github.nickid2018.koishibot.permission.UserPermissionEntry;
import io.github.nickid2018.koishibot.util.AsyncUtil;
import io.github.nickid2018.koishibot.util.MessageUtil;

import java.util.Arrays;
import java.util.Date;

public class PermissionResolver extends MessageResolver {

    public PermissionResolver() {
        super("~perm");
    }

    @Override
    public boolean needAt() {
        return true;
    }

    @Override
    public PermissionLevel getPermissionLevel() {
        return PermissionLevel.ADMIN;
    }

    // ~perm query
    // ~perm set
    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, Environment environment) {
        String[] dat = key.split(" ");
        if (dat.length < 1)
            return false;
        if (dat[0].equalsIgnoreCase("query")) {
            String user;
            if (dat.length == 2)
                user = environment.getEnvironmentUserPrefix() + dat[1];
            else
                user = Arrays.stream(MessageUtil.getAts(context.message()))
                        .map(AtMessage::getId).findFirst().orElse(null);
            if (user == null)
                return false;
            AsyncUtil.execute(() -> {
                UserPermissionEntry entry = PermissionManager.getPermissionEntry(user);
                String builder = "用户 " + entry.user() + "\n" +
                        "权限 " + entry.level().name() + "\n" +
                        "到期时间 " + (entry.expired() == Long.MAX_VALUE ? "无限期" : "%tc".formatted(new Date(entry.expired())));
                environment.getMessageSender().sendMessage(context, environment.newText(builder));
            });
            return true;
        } else if (dat[0].equalsIgnoreCase("set")) {
            if (dat.length < 3)
                return false;
            PermissionLevel level = PermissionLevel.valueOf(dat[1]);
            String expired = dat[2];
            long time = -1;
            if (expired.equalsIgnoreCase("+"))
                time = Long.MAX_VALUE;
            else if (expired.startsWith("+"))
                time = System.currentTimeMillis() + Long.parseLong(expired);
            if (time < 0)
                return false;
            String user;
            if (dat.length == 4)
                user = environment.getEnvironmentUserPrefix() + dat[3];
            else
                user = Arrays.stream(MessageUtil.getAts(context.message()))
                        .map(AtMessage::getId).findFirst().orElse(null);
            if (user == null)
                return false;
            long finalTime = time;
            AsyncUtil.execute(() -> {
                if (PermissionManager.tryGrantPermission(context.user().getUserId(), user, level, finalTime)) {
                    UserPermissionEntry entry = PermissionManager.getPermissionEntry(user);
                    String builder = "已设置成功\n用户 " + entry.user() + "\n" +
                            "权限 " + entry.level().name() + "\n" +
                            "到期时间 " + (entry.expired() == Long.MAX_VALUE ? "无限期" : "%tc".formatted(new Date(entry.expired())));
                    environment.getMessageSender().sendMessage(context, environment.newText(builder));
                } else
                    environment.getMessageSender().sendMessage(context, environment.newText("权限不足"));
            });
            return true;
        } else return false;
    }
}
