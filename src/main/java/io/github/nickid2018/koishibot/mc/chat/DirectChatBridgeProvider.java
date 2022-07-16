package io.github.nickid2018.koishibot.mc.chat;

import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import nl.vv32.rcon.Rcon;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.function.BiConsumer;

public class DirectChatBridgeProvider implements ChatBridgeProvider {

    private final InetSocketAddress addr;
    private final String password;
    private Rcon rcon;

    public DirectChatBridgeProvider(InetSocketAddress addr, String password) {
        this.addr = addr;
        this.password = password;
        tryLinkAndAuthenticate();
    }

    private boolean tryLinkAndAuthenticate() {
        try {
            rcon = Rcon.open(addr);
            rcon.authenticate(password);
            return true;
        } catch (IOException e) {
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
        commandBuilder.append("]\"},{\"color\":\"white\",\"text\":\"<");
        commandBuilder.append(user.getName());
        commandBuilder.append(">\"},{\"color\":\"white\",\"text\":\"");
        commandBuilder.append(text);
        commandBuilder.append("\"}]");
        do {
            loop = !loop;
            try {
                rcon.sendCommand(commandBuilder.toString());
                break;
            } catch (IOException ignored) {
            }
        } while (tryLinkAndAuthenticate() && loop);
    }

    @Override
    public void receiveMessage(BiConsumer<String, String> consumer) {
        // Unsupported now
    }
}
