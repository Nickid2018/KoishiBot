package io.github.nickid2018.koishibot.module.mc.chat;

import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import nl.vv32.rcon.Rcon;
import nl.vv32.rcon.RconBuilder;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

public class DirectChatBridgeProvider implements ChatBridgeProvider {

    private final InetSocketAddress addr;
    private final String password;
    private Rcon rcon;
    private boolean lastFailed = false;

    private final Set<BiConsumer<String, String>> listeners = new HashSet<>();

    public DirectChatBridgeProvider(InetSocketAddress addr, String password) {
        this.addr = addr;
        this.password = password;
        tryLinkAndAuthenticate();
    }

    private boolean tryLinkAndAuthenticate() {
        try {
            rcon = new RconBuilder().withCharset(StandardCharsets.UTF_8).withChannel(SocketChannel.open(addr)).build();
            rcon.authenticate(password);
            lastFailed = false;
            return true;
        } catch (Exception e) {
            if (!lastFailed) {
                lastFailed = true;
                MCChatBridgeModule.INSTANCE.getSendGroups(this).forEach(
                        u -> listeners.forEach(l -> l.accept(u, "警告：远端MC服务器无法连接")));
            }
            return false;
        }
    }

    @Override
    public void sendMessage(GroupInfo group, UserInfo user, String text) {
        boolean loop = false;
        StringBuilder commandBuilder = new StringBuilder();
        commandBuilder.append("tellraw @e[type=player] ");
        commandBuilder.append("[{\"color\":\"aqua\",\"text\":\"[");
        commandBuilder.append(group.getEnvironment().getEnvironmentName());
        commandBuilder.append("] \"},{\"color\":\"white\",\"text\":\"<");
        commandBuilder.append(user.getNameInGroup(group));
        commandBuilder.append("> \"},{\"color\":\"white\",\"text\":\"");
        commandBuilder.append(text);
        commandBuilder.append("\"}]");
        do {
            loop = !loop;
            try {
                rcon.sendCommand(commandBuilder.toString());
                break;
            } catch (Exception ignored) {
            }
        } while (tryLinkAndAuthenticate() && loop);
    }

    @Override
    public void receiveMessage(BiConsumer<String, String> consumer) {
        listeners.add(consumer);
    }
}
