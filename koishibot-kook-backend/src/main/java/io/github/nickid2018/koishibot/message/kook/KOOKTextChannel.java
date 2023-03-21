package io.github.nickid2018.koishibot.message.kook;

import io.github.kookybot.contract.TextChannel;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.network.ByteData;

public class KOOKTextChannel extends GroupInfo {
    private TextChannel channel;

    public KOOKTextChannel(KOOKEnvironment environment) {
        super(environment);
    }

    public KOOKTextChannel(Environment environment, TextChannel channel) {
        super(environment);
        this.channel = channel;
        name = channel.getName();
        groupId = "kook.group" + channel.getId();
    }

    public TextChannel getChannel() {
        return channel;
    }

    @Override
    public void read(ByteData buf) {
        super.read(buf);
        channel = ((KOOKEnvironment) env).getChannel(groupId);
    }
}
