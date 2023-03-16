package io.github.nickid2018.koishibot.module.system;

import io.github.nickid2018.koishibot.core.TempFileSystem;
import io.github.nickid2018.koishibot.message.DelegateEnvironment;
import io.github.nickid2018.koishibot.message.MessageResolver;
import io.github.nickid2018.koishibot.message.ResolverName;
import io.github.nickid2018.koishibot.message.Syntax;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.permission.PermissionLevel;
import io.github.nickid2018.koishibot.util.AsyncUtil;

@ResolverName("clean-cache")
@Syntax(syntax = "~cleancache", help = "清除bot缓存")
public class CleanCacheResolver extends MessageResolver {

    public CleanCacheResolver() {
        super("~cleancache");
    }

    @Override
    public PermissionLevel getPermissionLevel() {
        return PermissionLevel.ADMIN;
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, DelegateEnvironment environment) {
        if (!key.isEmpty())
            return false;
        TempFileSystem.cleanCache();
        AsyncUtil.execute(() -> environment.getMessageSender().sendMessage(context, environment.newText("缓存已清空")));
        return true;
    }
}
