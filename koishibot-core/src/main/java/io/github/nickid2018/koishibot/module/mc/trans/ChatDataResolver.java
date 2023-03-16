package io.github.nickid2018.koishibot.module.mc.trans;

import io.github.nickid2018.koishibot.util.tcp.SecServer;
import nl.vv32.rcon.Rcon;
import nl.vv32.rcon.RconBuilder;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class ChatDataResolver implements Consumer<byte[]> {

    private final InetSocketAddress addr;
    private final String password;
    private Rcon rcon;

    private SecServer server;

    private boolean lastFailed = false;

    public ChatDataResolver(InetSocketAddress addr, String password) {
        this.addr = addr;
        this.password = password;
        tryLinkAndAuthenticate();
    }

    public void setServer(SecServer server) {
        this.server = server;
    }

    private boolean tryLinkAndAuthenticate() {
        try {
            rcon = new RconBuilder().withCharset(StandardCharsets.UTF_8).withChannel(SocketChannel.open(addr)).build();
            rcon.authenticate(password);
            lastFailed = false;
            return true;
        } catch (Exception e) {
            if (!lastFailed && server != null) {
                lastFailed = true;
                byte[] data = "警告：远端MC服务器无法连接".getBytes(StandardCharsets.UTF_8);
                server.getServerHandlers().forEach(h -> {
                    try {
                        h.send(data);
                    } catch (Exception ignored) {
                    }
                });
            }
            return false;
        }
    }

    @Override
    public void accept(byte[] bytes) {
        try {
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes));
            String envName = input.readUTF();
            String user = input.readUTF();
            String text = input.readUTF();
            boolean loop = false;
            StringBuilder commandBuilder = new StringBuilder();
            commandBuilder.append("tellraw @e[type=player] ");
            commandBuilder.append("[{\"color\":\"aqua\",\"text\":\"[");
            commandBuilder.append(envName);
            commandBuilder.append("] \"},{\"color\":\"white\",\"text\":\"<");
            commandBuilder.append(user);
            commandBuilder.append("> \"},{\"color\":\"white\",\"text\":\"");
            commandBuilder.append(text);
            commandBuilder.append("\"}]");
            do {
                loop = !loop;
                try {
                    System.out.println("Debug: " + rcon.sendCommand(commandBuilder.toString()));
                    break;
                } catch (Exception ignored) {
                }
            } while (tryLinkAndAuthenticate() && loop);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
