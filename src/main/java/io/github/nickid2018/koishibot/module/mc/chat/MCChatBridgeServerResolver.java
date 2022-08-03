package io.github.nickid2018.koishibot.module.mc.chat;

import com.google.common.net.HostAndPort;
import io.github.nickid2018.koishibot.module.mc.chat.ChatBridgeSetting;
import io.github.nickid2018.koishibot.module.mc.chat.MCChatBridgeModule;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.resolver.MessageResolver;
import io.github.nickid2018.koishibot.util.AsyncUtil;
import io.github.nickid2018.koishibot.util.MessageUtil;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MCChatBridgeServerResolver extends MessageResolver {

    public MCChatBridgeServerResolver() {
        super("~mcchat server");
    }

    @Override
    public boolean groupEnabled() {
        return false;
    }

    @Override
    public boolean groupTempChat() {
        return true;
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
                    environment.getMessageSender().sendMessageAwait(context, environment.newText("""
                            选择创建服务器连接的类型：
                            0 - rcon直连，建议只在服务器和bot在同一个服务器上使用
                            1 - rcon间接连接，需要目标服务器安装适配器
                            其他 - 取消操作
                            """), (source, chain) -> {
                        String operation = MessageUtil.getFirstText(chain);
                        if (operation != null && operation.equals("0")) {
                            environment.getMessageSender().sendMessageAwait(context, environment.newText("输入RCON密码"),
                                    (sent, reply) -> {
                                        String password = MessageUtil.getFirstText(reply);
                                        try {
                                            MCChatBridgeModule.INSTANCE.addServer(new ChatBridgeSetting(
                                                    ChatBridgeSetting.BridgeType.DIRECT, addr, password));
                                            environment.getMessageSender().sendMessage(context, environment.newText("已添加服务器"));
                                        } catch (IOException e) {
                                            environment.getMessageSender().onError(e, "mc.server.chat", context, false);
                                        }
                                    });
                        } else if (operation != null && operation.equals("1")) {
                            try {
                                MCChatBridgeModule.INSTANCE.addServer(new ChatBridgeSetting(
                                        ChatBridgeSetting.BridgeType.INDIRECT, addr, null));
                                environment.getMessageSender().sendMessage(context, environment.newText("已添加服务器"));
                            } catch (IOException e) {
                                environment.getMessageSender().onError(e, "mc.server.chat", context, false);
                            }
                        } else
                            environment.getMessageSender().sendMessage(context, environment.newText("已取消操作"));
                    });
                } else {
                    MCChatBridgeModule.INSTANCE.removeServer(addr);
                    environment.getMessageSender().sendMessage(context, environment.newText("已删除服务器"));
                }
            } catch (Exception e) {
                environment.getMessageSender().onError(e, "mc.server.chat", context, false);
            }
        });
        return true;
    }
}
