package io.github.nickid2018.koishibot.module.system;

import io.github.nickid2018.koishibot.message.DelegateEnvironment;
import io.github.nickid2018.koishibot.message.MessageResolver;
import io.github.nickid2018.koishibot.message.ResolverName;
import io.github.nickid2018.koishibot.message.Syntax;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.permission.PermissionLevel;
import io.github.nickid2018.koishibot.util.AsyncUtil;

@ResolverName("runtime-arg")
@Syntax(syntax = "~runtime [参数名] [数据]", help = "设置运行时属性")
public class RuntimeArgResolver extends MessageResolver {

    public RuntimeArgResolver() {
        super("~runtime");
    }

    @Override
    public PermissionLevel getPermissionLevel() {
        return PermissionLevel.OWNER;
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, DelegateEnvironment environment) {
        String[] split = key.split(" ", 2);
        if (split.length != 2)
            return false;
        System.setProperty(split[0], split[1]);
        AsyncUtil.execute(() -> environment.getMessageSender().sendMessage(context, environment.newText("设置成功。")));
        return true;
    }
}
