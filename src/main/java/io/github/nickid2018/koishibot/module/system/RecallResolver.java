package io.github.nickid2018.koishibot.module.system;

import io.github.nickid2018.koishibot.message.MessageResolver;
import io.github.nickid2018.koishibot.message.ResolverName;
import io.github.nickid2018.koishibot.message.Syntax;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.message.api.QuoteMessage;
import io.github.nickid2018.koishibot.permission.PermissionLevel;
import io.github.nickid2018.koishibot.util.AsyncUtil;
import io.github.nickid2018.koishibot.util.MessageUtil;

@ResolverName("recall")
@Syntax(syntax = "~recall", help = "使bot撤回自己的信息")
public class RecallResolver extends MessageResolver {

    public RecallResolver() {
        super("~recall");
    }

    @Override
    public PermissionLevel getPermissionLevel() {
        return PermissionLevel.ADMIN;
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, Environment environment) {
        if (!key.isEmpty())
            return false;
        QuoteMessage quoteMessage = MessageUtil.getQuote(context.message());
        if (quoteMessage == null)
            return false;
        if (!quoteMessage.getReplyToID().equals(environment.getBotId()))
            return false;
        AsyncUtil.execute(quoteMessage.getQuoteFrom()::recall);
        return true;
    }
}
