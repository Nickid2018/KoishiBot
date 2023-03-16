package io.github.nickid2018.koishibot.module.mc.chat;

import com.google.common.net.HostAndPort;
import io.github.nickid2018.koishibot.message.DelegateEnvironment;
import io.github.nickid2018.koishibot.message.MessageResolver;
import io.github.nickid2018.koishibot.message.ResolverName;
import io.github.nickid2018.koishibot.message.Syntax;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.util.AsyncUtil;

import java.net.InetSocketAddress;

@ResolverName("mc-chat-listen")
@Syntax(syntax = "~mcchat add [服务器地址]", help = "添加群和服务器聊天链接", rem = "需要先添加服务器连接")
@Syntax(syntax = "~mcchat del [服务器地址]", help = "删除群和服务器聊天链接")
public class MCChatBridgeResolver extends MessageResolver {

    public MCChatBridgeResolver() {
        super("~mcchat");
    }

    @Override
    public boolean friendEnabled() {
        return false;
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, DelegateEnvironment environment) {
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
                    MCChatBridgeModule.INSTANCE.groupAddLink(context.group(), addr);
                    environment.getMessageSender().sendMessage(context, environment.newText("已添加连接"));
                } else {
                    MCChatBridgeModule.INSTANCE.groupRemoveLink(context.group(), addr);
                    environment.getMessageSender().sendMessage(context, environment.newText("已删除连接"));
                }
            } catch (Exception e) {
                environment.getMessageSender().onError(e, "mc.server.chat", context, false);
            }
        });
        return true;
    }
}
