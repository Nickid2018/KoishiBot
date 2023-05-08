package io.github.nickid2018.koishibot.module.mc;

import io.github.nickid2018.koishibot.message.DelegateEnvironment;
import io.github.nickid2018.koishibot.message.MessageResolver;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.permission.PermissionLevel;
import io.github.nickid2018.koishibot.util.AsyncUtil;

public class MCSubscribeResolver extends MessageResolver {

    public MCSubscribeResolver() {
        super("~mcs");
    }

    @Override
    public boolean friendEnabled() {
        return false;
    }

    @Override
    public boolean needAt() {
        return true;
    }

    @Override
    public PermissionLevel getPermissionLevel() {
        return PermissionLevel.ADMIN;
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, DelegateEnvironment environment) {
        key = key.trim().toLowerCase();
        boolean mark = key.charAt(0) == '+';
        key = key.substring(1);
        return switch (key) {
            case "version" -> {
                AsyncUtil.execute(() -> {
                    try {
                        MCModule.INSTANCE.subscribedGroups.updateData(context.group().getGroupId(), set -> {
                            if (mark)
                                set.add("version");
                            else
                                set.remove("version");
                            return set;
                        });
                        environment.getMessageSender().sendMessage(context, environment.newText(
                                mark ? "已订阅版本更新通知" : "已取消订阅版本更新通知"
                        ));
                    } catch (Exception e) {
                        environment.getMessageSender().onError(e, "mc", context, false);
                    }
                });
                yield true;
            }
            default -> false;
        };
    }
}
