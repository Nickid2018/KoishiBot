package io.github.nickid2018.koishibot.module.system;

import io.github.nickid2018.koishibot.core.MonitorListener;
import io.github.nickid2018.koishibot.message.DelegateEnvironment;
import io.github.nickid2018.koishibot.message.MessageResolver;
import io.github.nickid2018.koishibot.message.ResolverName;
import io.github.nickid2018.koishibot.message.Syntax;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.permission.PermissionLevel;

@Syntax(syntax = "~update", help = "更新bot")
@ResolverName("update")
public class UpdateResolver extends MessageResolver {

    public UpdateResolver() {
        super("~update");
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
        MonitorListener.doUpdate(environment, context);
        return true;
    }
}
