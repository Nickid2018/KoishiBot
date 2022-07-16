package io.github.nickid2018.koishibot.resolver;

import io.github.nickid2018.koishibot.mc.chat.MCChatBridge;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import kotlin.Pair;

public class MCChatListen extends MessageResolver {

    public MCChatListen() {
        super(s -> new Pair<>(s, s));
    }

    @Override
    public boolean friendEnabled() {
        return false;
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, Environment environment) {
        MCChatBridge.INSTANCE.onReceiveGroupText(context.group(), context.user(), key);
        return false;
    }
}
