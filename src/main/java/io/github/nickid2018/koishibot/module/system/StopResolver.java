package io.github.nickid2018.koishibot.module.system;

import io.github.nickid2018.koishibot.core.BotStart;
import io.github.nickid2018.koishibot.message.MessageResolver;
import io.github.nickid2018.koishibot.message.ResolverName;
import io.github.nickid2018.koishibot.message.Syntax;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.permission.PermissionLevel;

@ResolverName("stop")
@Syntax(syntax = "", help = "紧急停止bot")
public class StopResolver extends MessageResolver {

    public StopResolver() {
        super("~stop");
    }

    @Override
    public PermissionLevel getPermissionLevel() {
        return PermissionLevel.OWNER;
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, Environment environment) {
        if (!key.isEmpty())
            return false;
        environment.getMessageSender().sendMessage(context, environment.newText("进行紧急停止。"));
        BotStart.terminate();
        return true;
    }
}
