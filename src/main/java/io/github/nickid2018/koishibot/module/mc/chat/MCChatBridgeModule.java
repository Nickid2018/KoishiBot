package io.github.nickid2018.koishibot.module.mc.chat;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import io.github.nickid2018.koishibot.module.Module;
import io.github.nickid2018.koishibot.util.DataReader;
import io.github.nickid2018.koishibot.util.GroupDataReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Stream;

public class MCChatBridgeModule extends Module {

    public static final Logger CHAT_BRIDGE_LOGGER = LoggerFactory.getLogger("MC Chat Bridge");

    public static MCChatBridgeModule INSTANCE;

    private GroupDataReader<Set<InetSocketAddress>> groupChatBridges;
    private DataReader<Set<ChatBridgeSetting>> settings;

    private final Map<InetSocketAddress, ChatBridgeProvider> providerMap = new HashMap<>();
    private final Map<String, Set<ChatBridgeProvider>> groupMap = new HashMap<>();

    public MCChatBridgeModule()  {
        super("mcchat", List.of(new MCChatBridgeServerResolver(), new MCChatBridgeResolver()), true);
    }

    public void groupAddLink(GroupInfo group, InetSocketAddress addr) throws Exception {
        groupChatBridges.updateData(group.getGroupId(), set -> {
            set.add(addr);
            groupMap.computeIfAbsent(group.getGroupId(), s -> new HashSet<>()).add(providerMap.get(addr));
            return set;
        });
    }

    public void groupRemoveLink(GroupInfo group, InetSocketAddress addr) throws Exception {
        groupChatBridges.updateData(group.getGroupId(), set -> {
            set.remove(addr);
            groupMap.computeIfAbsent(group.getGroupId(), s -> new HashSet<>()).remove(providerMap.get(addr));
            return set;
        });
    }

    public void addServer(ChatBridgeSetting setting) throws IOException {
        settings.getData().add(setting);
        settings.saveData();
        ChatBridgeProvider provider = switch (setting.type) {
            case DIRECT -> new DirectChatBridgeProvider(setting.remote, setting.password);
            case INDIRECT -> new IndirectChatBridgeProvider(setting.remote);
        };
        providerMap.put(setting.remote, provider);
    }

    public void removeServer(InetSocketAddress addr) throws IOException {
        settings.getData().removeIf(setting -> setting.remote.equals(addr));
        settings.saveData();
        providerMap.remove(addr);
    }

    public void onReceiveGroupText(GroupInfo group, UserInfo user, String text) {
        String[] textArray = text.split("\n");
        Stream.of(textArray).limit(5)
                .map(s -> s.length() > 100 ? s.substring(0, 100) + "..." : s)
                .map(s -> s.replace("\"", "\\\""))
                .forEach(t -> groupMap.computeIfAbsent(group.getGroupId(), s -> new HashSet<>())
                        .forEach(provider -> provider.sendMessage(group, user, t)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onStartInternal() throws Exception {
        groupChatBridges = new GroupDataReader<>("mcchat",
                reader -> (Set<InetSocketAddress>) new ObjectInputStream(reader).readObject(),
                (writer, data) -> new ObjectOutputStream(writer).writeObject(data),
                HashSet::new);
        groupChatBridges.loadAll();

        settings = new DataReader<>(new File(groupChatBridges.getFolder(), "chat.settings"), HashSet::new);
        settings.getData().forEach(chatBridgeSetting -> {
            ChatBridgeProvider provider = switch (chatBridgeSetting.type) {
                case DIRECT -> new DirectChatBridgeProvider(chatBridgeSetting.remote, chatBridgeSetting.password);
                case INDIRECT -> new IndirectChatBridgeProvider(chatBridgeSetting.remote);
            };
            providerMap.put(chatBridgeSetting.remote, provider);
        });

        groupChatBridges.getGroups().forEach(group -> groupChatBridges.getData(group).forEach(
                addr -> groupMap.computeIfAbsent(group, s -> new HashSet<>()).add(providerMap.get(addr))));

        INSTANCE = this;
    }

    @Override
    public void onSettingReloadInternal(JsonObject settingRoot) {
    }

    @Override
    public void onTerminateInternal() {
    }

    @Override
    public String getDescription() {
        return "MC聊天连接相关模块";
    }

    @Override
    public String getSummary() {
        return """
                提供与MC之间的聊天转发。
                有两种方式可以提供转发，都需要使用RCON。
                1. 直连RCON，如果bot和服务器不在一个服务器上不应该使用这个方式。
                2. 经转发器转发，这时服务器地址要填入转发器接口地址。
                """;
    }
}
