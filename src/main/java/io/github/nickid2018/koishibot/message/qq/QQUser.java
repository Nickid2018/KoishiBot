package io.github.nickid2018.koishibot.message.qq;

import io.github.nickid2018.koishibot.message.api.ContactInfo;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import net.mamoe.mirai.contact.Stranger;
import net.mamoe.mirai.contact.User;

public class QQUser implements UserInfo {

    private final Environment environment;
    private final User user;
    private final boolean stranger;
    private final boolean group;

    public QQUser(Environment environment, User user, boolean stranger, boolean group) {
        this.environment = environment;
        this.user = user;
        this.stranger = stranger;
        this.group = group;
    }

    @Override
    public String getUserId() {
        return "qq.user" + user.getId();
    }

    public User getUser() {
        return user;
    }

    public boolean isGroup() {
        return group;
    }

    public boolean isStranger() {
        return stranger || user instanceof Stranger;
    }

    @Override
    public void nudge(ContactInfo contact) {
        if (contact instanceof QQUser)
            user.nudge().sendTo(((QQUser) contact).getUser());
        else
            user.nudge().sendTo(((QQGroup) contact).getGroup());
    }

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public boolean equals(ContactInfo info) {
        if (!(info instanceof QQUser))
            return false;
        return user.getId() == ((QQUser) info).user.getId();
    }

    @Override
    public String getName() {
        return user.getNick();
    }
}
