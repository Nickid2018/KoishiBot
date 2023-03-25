package io.github.nickid2018.koishibot.module.system;

import io.github.nickid2018.koishibot.core.MonitorListener;
import io.github.nickid2018.koishibot.message.DelegateEnvironment;
import io.github.nickid2018.koishibot.message.MessageResolver;
import io.github.nickid2018.koishibot.message.ResolverName;
import io.github.nickid2018.koishibot.message.Syntax;
import io.github.nickid2018.koishibot.message.api.MessageContext;

@Syntax(syntax = "~check_update", help = "检查bot更新")
@ResolverName("check_action")
public class CheckActionResolver extends MessageResolver {

    public CheckActionResolver() {
        super("~check_update");
    }

    @Override
    public boolean needAt() {
        return true;
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, DelegateEnvironment environment) {
        MonitorListener.checkActionID(environment, context);
        return true;
    }
}
