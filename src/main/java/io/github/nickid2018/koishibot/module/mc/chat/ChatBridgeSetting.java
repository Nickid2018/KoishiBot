package io.github.nickid2018.koishibot.module.mc.chat;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class ChatBridgeSetting implements Serializable {

    public InetSocketAddress remote;
    public String password;

    public BridgeType type;

    public ChatBridgeSetting(BridgeType type, InetSocketAddress addr, String password) {
        this.type = type;
        this.password = password;
        this.remote = addr;
    }

    public enum BridgeType {
        DIRECT,
        INDIRECT
    }
}
