package io.github.nickid2018.koishibot.filter;

import io.github.nickid2018.koishibot.message.api.*;
import io.github.nickid2018.koishibot.permission.PermissionLevel;
import io.github.nickid2018.koishibot.permission.PermissionManager;
import io.github.nickid2018.koishibot.util.value.MutableBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RequestFrequencyFilter implements PreFilter, PostFilter {

    private static final Map<UserInfo, Long> USER_REQUEST_TIME = new ConcurrentHashMap<>();
    private static final Map<UserInfo, Integer> USER_REQUEST_FAIL = new ConcurrentHashMap<>();

    private static final int REQUEST_MAX_FAIL = 5;
    private static final long REQUEST_DURATION = 3000;
    private static final long AUTO_BAN_TIME = 3600_000;

    public static synchronized boolean shouldNotResponse(UserInfo member, MutableBoolean nowBan) {
        long nowTime = System.currentTimeMillis();
        if (USER_REQUEST_TIME.containsKey(member) && nowTime - USER_REQUEST_TIME.get(member) <= REQUEST_DURATION) {
            int times = USER_REQUEST_FAIL.getOrDefault(member, 0);
            times++;
            if (times >= REQUEST_MAX_FAIL) {
                PermissionManager.setLevel(member.getUserId(), PermissionLevel.BANNED, nowTime + AUTO_BAN_TIME, false);
                USER_REQUEST_FAIL.remove(member);
                nowBan.setValue(true);
            } else
                USER_REQUEST_FAIL.put(member, times);
            USER_REQUEST_TIME.put(member, nowTime);
            return true;
        }
        USER_REQUEST_FAIL.remove(member);
        return false;
    }

    public static synchronized void refreshRequestTime(UserInfo member) {
        if (member != null)
            USER_REQUEST_TIME.put(member, System.currentTimeMillis());
    }

    @NotNull
    @Override
    public AbstractMessage filterMessagePost(AbstractMessage input, MessageContext context, Environment environment) {
        refreshRequestTime(context.user());
        if (Math.random() < 0.2 && context.user() != null)
            context.user().nudge(context.group() != null ? context.group() : context.user());
        return input;
    }

    @Nullable
    @Override
    public ChainMessage filterMessagePre(ChainMessage input, MessageContext context, Environment environment) {
        MutableBoolean ban = new MutableBoolean(false);
        if (shouldNotResponse(context.user(), ban)) {
            if (ban.getValue()) {
                if (context.group() != null)
                    environment.getMessageSender().sendMessage(context, environment.newChain(
                            environment.newAt(context.group(), context.user()),
                            environment.newText(" 被自动封禁一小时，原因: 过于频繁的操作")
                    ));
                else
                    environment.getMessageSender().sendMessage(context,
                            environment.newText("被自动封禁一小时，原因: 过于频繁的操作"));
            }
            return null;
        }
        return input;
    }
}
