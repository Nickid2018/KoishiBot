package io.github.nickid2018.koishibot.message.telegram;

import io.github.nickid2018.koishibot.message.api.ContactInfo;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import org.telegram.telegrambots.meta.api.objects.User;

public class TelegramUser implements UserInfo {

    private final TelegramEnvironment environment;
    private final User user;

    public TelegramUser(TelegramEnvironment environment, User user) {
        this.environment = environment;
        this.user = user;
    }

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public boolean equals(ContactInfo info) {
        return info instanceof TelegramUser && ((TelegramUser) info).user.getId().equals(user.getId());
    }

    @Override
    public String getName() {
        return user.getUserName();
    }

    @Override
    public String getUserId() {
        return "tg.user" + user.getId();
    }

    @Override
    public boolean isStranger() {
        return false;
    }

    @Override
    public void nudge(ContactInfo contact) {
        // Unsupported
    }

    @Override
    public String getNameInGroup(GroupInfo group) {
        return getName();
    }

    public User getUser() {
        return user;
    }
}
