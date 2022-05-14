package io.github.nickid2018.koishibot.core;

import net.mamoe.mirai.message.MessageReceipt;

public class MessageData {

    public final MessageInfo info;
    public final MessageReceipt<?> receipt;

    public MessageData(MessageInfo info, MessageReceipt<?> receipt) {
        this.info = info;
        this.receipt = receipt;
    }

    public boolean equals(MessageInfo o) {
        if (o.group == null ^ info.group == null)
            return false;
        if (o.group != null && !o.group.equals(info.group))
            return false;
        if (o.friend == null ^ info.friend == null)
            return false;
        if (o.friend != null && !o.friend.equals(info.friend))
            return false;
        if (o.sender == null ^ info.sender == null)
            return false;
        if (o.sender != null && !o.sender.equals(info.sender))
            return false;
        if (o.stranger == null ^ info.stranger == null)
            return false;
        return o.stranger == null || o.stranger.equals(info.stranger);
    }
}
