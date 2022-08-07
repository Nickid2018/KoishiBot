package io.github.nickid2018.koishibot.message.kook;

import io.github.kookybot.contract.GuildUser;
import io.github.kookybot.contract.User;
import io.github.nickid2018.koishibot.message.api.ContactInfo;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import io.github.nickid2018.koishibot.util.value.Either;

public class KOOKUser implements UserInfo {

    private final Environment environment;
    private final Either<User, GuildUser> user;
    private final boolean group;

    public KOOKUser(Environment environment, User user, boolean group) {
        this.environment = environment;
        this.user = Either.left(user);
        this.group = group;
    }

    public KOOKUser(Environment environment, GuildUser user, boolean group) {
        this.environment = environment;
        this.user = Either.right(user);
        this.group = group;
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
        return user.isLeft() ? user.left().getName() : user.right().getName();
    }

    @Override
    public String getUserId() {
        return "kook.user" + (user.isLeft() ? user.left().getId() : user.right().getId());
    }

    @Override
    public boolean isStranger() {
        return group;
    }

    public Either<User, GuildUser> getUser() {
        return user;
    }

    @Override
    public void nudge(ContactInfo contact) {
        // Unsupported
    }

    @Override
    public String getNameInGroup(GroupInfo group) {
        if (group instanceof KOOKTextChannel channel) {
            GuildUser u = channel.getChannel().getGuild().getGuildUser(
                    user.isLeft() ? user.left().getId() : user.right().getId());
            return u == null ? getName() : u.getName();
        } else
            return null;
    }
}
