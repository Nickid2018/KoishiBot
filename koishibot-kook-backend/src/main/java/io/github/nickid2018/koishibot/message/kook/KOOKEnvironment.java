package io.github.nickid2018.koishibot.message.kook;

import io.github.kookybot.client.Client;
import io.github.kookybot.contract.Channel;
import io.github.kookybot.contract.GuildUser;
import io.github.kookybot.contract.Self;
import io.github.kookybot.contract.TextChannel;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import io.github.nickid2018.koishibot.network.Connection;

import java.util.Objects;

public class KOOKEnvironment extends Environment {

    private final Client kookClient;
    private final Self self;
    private final KOOKMessagePublisher publisher;

    public KOOKEnvironment(Client kookClient, Self self, Connection connection) {
        super(connection);
        this.kookClient = kookClient;
        this.self = self;

        publisher = new KOOKMessagePublisher(this);

        botID = "kook.user" + self.getId();
        forwardMessageSupported = false;
        audioSupported = false;
        audioToFriendSupported = false;
        quoteSupported = true;
        environmentName = "开黑啦";
        environmentUserPrefix = "kook.user";
        needAntiFilter = false;
        audioSilk = false;
    }

    public GuildUser getUser(String id) {
        return self.getGuilds().values().stream()
                .map(guild -> guild.getGuildUser(id))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @Override
    public UserInfo getUser(String id, boolean isStranger) {
        if (!id.startsWith("kook.user"))
            return null;
        String user = id.substring(9);
        return self.getGuilds().values().stream()
                .map(guild -> guild.getGuildUser(user))
                .filter(Objects::nonNull)
                .map(guildUser -> new KOOKUser(this, guildUser))
                .findFirst()
                .orElse(null);
    }

    public TextChannel getChannel(String id) {
        if (!id.startsWith("kook.group"))
            return null;
        Channel channel = self.getChannel(id.substring(10));
        return channel instanceof TextChannel ? (TextChannel) channel : null;
    }

    @Override
    public GroupInfo getGroup(String id) {
        TextChannel channel = getChannel(id);
        return channel != null ? new KOOKTextChannel(this, channel) : null;
    }

    @Override
    public void close() {
        kookClient.close();
    }

    public Client getKookClient() {
        return kookClient;
    }

    public Self getSelf() {
        return self;
    }
}
