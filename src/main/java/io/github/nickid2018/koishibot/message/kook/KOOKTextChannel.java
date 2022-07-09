package io.github.nickid2018.koishibot.message.kook;

import io.github.nickid2018.koishibot.message.api.ContactInfo;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.zly2006.kookybot.contract.TextChannel;

public class KOOKTextChannel implements GroupInfo {

    private final TextChannel channel;

    public KOOKTextChannel(TextChannel channel) {
        this.channel = channel;
    }

    public TextChannel getChannel() {
        return channel;
    }

    @Override
    public boolean equals(ContactInfo info) {
        return info instanceof KOOKTextChannel otherGroup && otherGroup.channel.equals(channel);
    }

    @Override
    public String getGroupId() {
        return "kook.group" + channel.getId();
    }
}
