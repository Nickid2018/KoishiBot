package io.github.nickid2018.koishibot.message.kook;

import io.github.kookybot.contract.TextChannel;
import io.github.nickid2018.koishibot.message.api.ContactInfo;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.GroupInfo;

public class KOOKTextChannel implements GroupInfo {

    private final Environment environment;
    private final TextChannel channel;

    public KOOKTextChannel(Environment environment, TextChannel channel) {
        this.environment = environment;
        this.channel = channel;
    }

    public TextChannel getChannel() {
        return channel;
    }

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public boolean equals(ContactInfo info) {
        return info instanceof KOOKTextChannel otherGroup && otherGroup.channel.equals(channel);
    }

    @Override
    public String getName() {
        return channel.getName();
    }

    @Override
    public String getGroupId() {
        return "kook.group" + channel.getId();
    }
}
