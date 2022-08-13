package io.github.nickid2018.koishibot.module.system;

import io.github.nickid2018.koishibot.message.MessageResolver;
import io.github.nickid2018.koishibot.message.ResolverName;
import io.github.nickid2018.koishibot.message.Syntax;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.module.ModuleManager;
import io.github.nickid2018.koishibot.permission.PermissionLevel;
import io.github.nickid2018.koishibot.util.AsyncUtil;

@ResolverName("reload")
@Syntax(syntax = "~reload", help = "重新加载所有模块")
public class ReloadResolver extends MessageResolver {

    public ReloadResolver() {
        super("~reload");
    }

    @Override
    public PermissionLevel getPermissionLevel() {
        return PermissionLevel.ADMIN;
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, Environment environment) {
        AsyncUtil.execute(() -> {
            try {
                for (String name : ModuleManager.getModuleNames())
                    ModuleManager.reload(name);
                environment.getMessageSender().sendMessage(context, environment.newText("已重载所有模块"));
            } catch (Exception e) {
                environment.getMessageSender().onError(e, "module.reload", context, false);
            }
        });
        return true;
    }
}
