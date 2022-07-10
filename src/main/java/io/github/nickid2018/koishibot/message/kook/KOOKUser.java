package io.github.nickid2018.koishibot.message.kook;

import io.github.nickid2018.koishibot.message.api.ContactInfo;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import io.github.nickid2018.koishibot.util.value.Either;
import io.github.zly2006.kookybot.contract.GuildUser;
import io.github.zly2006.kookybot.contract.User;

public class KOOKUser implements UserInfo {

    private final Either<User, GuildUser> user;
    private final boolean group;

    public KOOKUser(User user, boolean group) {
        this.user = Either.left(user);
        this.group = group;
    }

    public KOOKUser(GuildUser user, boolean group) {
        this.user = Either.right(user);
        this.group = group;
    }

    @Override
    public boolean equals(ContactInfo info) {
        return info instanceof KOOKUser otherUser && otherUser.user.equals(user);
    }

    @Override
    public String getUserId() {
        return "kook.user" + (user.isLeft() ? user.left() : user.right()).getId();
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
}
