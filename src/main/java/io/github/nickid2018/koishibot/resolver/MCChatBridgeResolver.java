package io.github.nickid2018.koishibot.resolver;

import com.google.common.net.HostAndPort;
import io.github.nickid2018.koishibot.mc.chat.MCChatBridge;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.util.AsyncUtil;

import java.net.InetSocketAddress;

public class MCChatBridgeResolver extends MessageResolver {

    public MCChatBridgeResolver() {
        super("~mcchat");
    }

    @Override
    public boolean friendEnabled() {
        return false;
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, Environment environment) {
        String[] split = key.split(" ", 2);
        if (split.length != 2)
            return false;
        if (!split[0].equalsIgnoreCase("add") && !split[0].equalsIgnoreCase("del"))
            return false;
        AsyncUtil.execute(() -> {
            try {
                HostAndPort hostAndPort = HostAndPort.fromString(split[1]);
                InetSocketAddress addr = new InetSocketAddress(hostAndPort.getHost(), hostAndPort.getPort());
                if (split[0].equalsIgnoreCase("add")) {
                    MCChatBridge.INSTANCE.groupAddLink(context.group(), addr);
                    environment.getMessageSender().sendMessage(context, environment.newText("已添加连接"));
                } else {
                    MCChatBridge.INSTANCE.groupRemoveLink(context.group(), addr);
                    environment.getMessageSender().sendMessage(context, environment.newText("已删除连接"));
                }
            } catch (Exception e) {
                environment.getMessageSender().onError(e, "mc.server.chat", context, false);
            }
        });
        return true;
    }
}
