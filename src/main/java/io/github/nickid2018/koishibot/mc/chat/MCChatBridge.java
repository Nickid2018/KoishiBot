package io.github.nickid2018.koishibot.mc.chat;

import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import io.github.nickid2018.koishibot.util.DataReader;
import io.github.nickid2018.koishibot.util.GroupDataReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MCChatBridge {

    public static final Logger CHAT_BRIDGE_LOGGER = LoggerFactory.getLogger("MC Chat Bridge");

    public static MCChatBridge INSTANCE;

    public static void init() {
        try {
            INSTANCE = new MCChatBridge();
        } catch (Exception e) {
            CHAT_BRIDGE_LOGGER.error("Can't initialize MC Chat Bridge.", e);
        }
    }

    private final GroupDataReader<Set<InetSocketAddress>> groupChatBridges;
    private final DataReader<Set<ChatBridgeSetting>> settings;

    private final Map<InetSocketAddress, ChatBridgeProvider> providerMap = new HashMap<>();
    private final Map<String, Set<ChatBridgeProvider>> groupMap = new HashMap<>();

    @SuppressWarnings("unchecked")
    private MCChatBridge() throws IOException {
        groupChatBridges = new GroupDataReader<>("mcchat",
                reader -> (Set<InetSocketAddress>) new ObjectInputStream(reader).readObject(),
                (writer, data) -> new ObjectOutputStream(writer).writeObject(data),
                HashSet::new);
        groupChatBridges.loadAll();

        settings = new DataReader<>(new File(groupChatBridges.getFolder(), "chat.settings"), HashSet::new);
        settings.getData().forEach(chatBridgeSetting -> {
            ChatBridgeProvider provider = switch (chatBridgeSetting.type) {
                case DIRECT -> new DirectChatBridgeProvider(chatBridgeSetting.remote, chatBridgeSetting.password);
                default -> null;
            };
            providerMap.put(chatBridgeSetting.remote, provider);
        });

        groupChatBridges.getGroups().forEach(group -> groupChatBridges.getData(group).forEach(
                addr -> groupMap.computeIfAbsent(group, s -> new HashSet<>()).add(providerMap.get(addr))));
    }

    public void groupAddLink(GroupInfo group, InetSocketAddress addr) throws Exception {
        groupChatBridges.updateData(group.getGroupId(), set -> {
            set.add(addr);
            return set;
        });
    }

    public void groupRemoveLink(GroupInfo group, InetSocketAddress addr) throws Exception {
        groupChatBridges.updateData(group.getGroupId(), set -> {
            set.remove(addr);
            return set;
        });
    }

    public void addServer(ChatBridgeSetting setting) throws IOException {
        settings.getData().add(setting);
        settings.saveData();
        ChatBridgeProvider provider = switch (setting.type) {
            case DIRECT -> new DirectChatBridgeProvider(setting.remote, setting.password);
            default -> null;
        };
        providerMap.put(setting.remote, provider);
    }

    public void removeServer(InetSocketAddress addr) throws IOException {
        settings.getData().removeIf(setting -> setting.remote.equals(addr));
        settings.saveData();
        providerMap.remove(addr);
    }

    public void onReceiveGroupText(GroupInfo group, UserInfo user, String text) {
        groupMap.computeIfAbsent(group.getGroupId(), s -> new HashSet<>()).forEach(
                provider -> provider.sendMessage(group, user, text));
    }
}
