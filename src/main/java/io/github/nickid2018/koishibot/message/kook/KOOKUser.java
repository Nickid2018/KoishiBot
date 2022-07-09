package io.github.nickid2018.koishibot.message.kook;

import io.github.nickid2018.koishibot.message.api.ContactInfo;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import io.github.zly2006.kookybot.contract.User;

public class KOOKUser implements UserInfo {

    private final User user;
    private final boolean group;

    public KOOKUser(User user, boolean group) {
        this.user = user;
        this.group = group;
    }

    @Override
    public boolean equals(ContactInfo info) {
        return info instanceof KOOKUser otherUser && otherUser.user.equals(user);
    }

    @Override
    public String getUserId() {
        return "kook.user" + user.getId();
    }

    @Override
    public boolean isStranger() {
        return group;
    }

    @Override
    public void nudge(ContactInfo contact) {
        // Unsupported
    }
}
