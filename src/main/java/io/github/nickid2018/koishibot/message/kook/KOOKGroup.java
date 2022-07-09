package io.github.nickid2018.koishibot.message.kook;

import io.github.nickid2018.koishibot.message.api.ContactInfo;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.zly2006.kookybot.contract.Channel;

public class KOOKGroup implements GroupInfo {

    private final Channel channel;

    public KOOKGroup(Channel channel) {
        this.channel = channel;
    }

    public Channel getChannel() {
        return channel;
    }

    @Override
    public boolean equals(ContactInfo info) {
        return info instanceof KOOKGroup otherGroup && otherGroup.channel.equals(channel);
    }

    @Override
    public String getGroupId() {
        return "kook.group" + channel.getId();
    }
}
