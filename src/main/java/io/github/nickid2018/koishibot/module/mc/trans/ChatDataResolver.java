package io.github.nickid2018.koishibot.module.mc.trans;

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

    public ChatDataResolver(InetSocketAddress addr, String password) {
        this.addr = addr;
        this.password = password;
        tryLinkAndAuthenticate();
    }

    private boolean tryLinkAndAuthenticate() {
        try {
            rcon = new RconBuilder().withCharset(StandardCharsets.UTF_8).withChannel(SocketChannel.open(addr)).build();
            rcon.authenticate(password);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
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
            System.out.println("DEBUG: " + commandBuilder);
            do {
                loop = !loop;
                try {
                    System.out.println("debug: " + rcon.sendCommand(commandBuilder.toString()));
                    break;
                } catch (Exception ignored) {
                }
            } while (tryLinkAndAuthenticate() && loop);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
