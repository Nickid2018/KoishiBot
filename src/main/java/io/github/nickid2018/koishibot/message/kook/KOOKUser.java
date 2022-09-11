package io.github.nickid2018.koishibot.message.kook;

import io.github.kookybot.contract.GuildUser;
import io.github.nickid2018.koishibot.message.api.ContactInfo;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.UserInfo;

public class KOOKUser implements UserInfo {

    private final Environment environment;
    private final GuildUser user;

    public KOOKUser(Environment environment, GuildUser user) {
        this.environment = environment;
        this.user = user;
    }

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public boolean equals(ContactInfo info) {
        return info instanceof KOOKUser otherUser && otherUser.user.equals(user);
    }

    @Override
    public String getName() {
        return user.getName();
    }

    @Override
    public String getUserId() {
        return "kook.user" + user.getId();
    }

    @Override
    public boolean isStranger() {
        return true;
    }

    public GuildUser getUser() {
        return user;
    }

    @Override
    public void nudge(ContactInfo contact) {
        // Unsupported
    }

    @Override
    public String getNameInGroup(GroupInfo group) {
        GuildUser u = ((KOOKTextChannel) group).getChannel().getGuild().getGuildUser(user.getId());
        return u == null ? getName() : u.getName();
    }
}
