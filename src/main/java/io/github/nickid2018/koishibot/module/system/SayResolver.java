package io.github.nickid2018.koishibot.module.system;

import io.github.nickid2018.koishibot.message.MessageResolver;
import io.github.nickid2018.koishibot.message.ResolverName;
import io.github.nickid2018.koishibot.message.Syntax;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.permission.PermissionLevel;
import io.github.nickid2018.koishibot.util.AsyncUtil;

@ResolverName("say")
@Syntax(syntax = "~say [一句话]", help = "bot复述这句话", rem = "只能复述文本，图片等不会复述，文本两边空格会被删除")
public class SayResolver extends MessageResolver {

    public SayResolver() {
        super("~say");
    }

    @Override
    public PermissionLevel getPermissionLevel() {
        return PermissionLevel.ADMIN;
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, Environment environment) {
        AsyncUtil.execute(() -> environment.getMessageSender().sendMessage(context, environment.newText(key)));
        return true;
    }
}
