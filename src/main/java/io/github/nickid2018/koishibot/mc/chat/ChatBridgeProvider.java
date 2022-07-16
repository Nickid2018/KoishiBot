package io.github.nickid2018.koishibot.mc.chat;

import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.UserInfo;

import java.util.function.BiConsumer;

public interface ChatBridgeProvider {

    void sendMessage(GroupInfo group, UserInfo user, String text);

    void receiveMessage(BiConsumer<String, String> consumer);
}
