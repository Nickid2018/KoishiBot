package io.github.nickid2018.koishibot.message.kook;

import io.github.nickid2018.koishibot.message.api.MessageFrom;
import io.github.zly2006.kookybot.contract.TextChannel;
import io.github.zly2006.kookybot.contract.User;

public class KOOKMessageFrom implements MessageFrom {

    private final TextChannel channel;
    private final User user;
    private final long timestamp;

    public KOOKMessageFrom(TextChannel channel, User user, long timestamp) {
        this.channel = channel;
        this.user = user;
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(MessageFrom source) {
        return source instanceof KOOKMessageFrom other &&
                ((channel == null && other.channel == null) ||
                        (channel != null && other.channel != null && channel.equals(other.channel))) &&
                user.equals(other.user) && timestamp == other.timestamp;
    }
}
