package io.github.nickid2018.koishibot.module.mc.chat;

import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import io.github.nickid2018.koishibot.util.tcp.SecClient;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

public class IndirectChatBridgeProvider implements ChatBridgeProvider {

    private final InetSocketAddress addr;
    private SecClient client;

    private final Set<BiConsumer<String, String>> listeners = new HashSet<>();

    public IndirectChatBridgeProvider(InetSocketAddress addr) {
        this.addr = addr;
        tryLink();
    }

    private boolean tryLink() {
        try {
            if (client != null)
                client.close();
        } catch (IOException ignored) {
        }
        try {
            client = new SecClient(addr, bytes -> {
                String data = new String(bytes, StandardCharsets.UTF_8);
                MCChatBridgeModule.INSTANCE.getSendGroups(this).forEach(
                        group -> listeners.forEach(consumer -> consumer.accept(group, data)));
            });
            MCChatBridgeModule.CHAT_BRIDGE_LOGGER.info("Connected remote transfer program.");
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void sendMessage(GroupInfo group, UserInfo user, String text) {
        boolean loop = false;
        String envName = group.getEnvironment().getEnvironmentName();
        String userName = user.getNameInGroup(group);
        ByteArrayOutputStream boas = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(boas);
        try {
            dos.writeUTF(envName);
            dos.writeUTF(userName);
            dos.writeUTF(text);
        } catch (IOException ignored) {
        }
        byte[] data = boas.toByteArray();
        do {
            loop = !loop;
            try {
                client.send(data);
                break;
            } catch (Exception ignored) {
            }
        } while (tryLink() && loop);
    }

    @Override
    public void receiveMessage(BiConsumer<String, String> consumer) {
        listeners.add(consumer);
    }
}
