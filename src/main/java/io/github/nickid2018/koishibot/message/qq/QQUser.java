package io.github.nickid2018.koishibot.message.qq;

import io.github.nickid2018.koishibot.message.api.ContactInfo;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.contact.Stranger;
import net.mamoe.mirai.contact.User;

public class QQUser implements UserInfo {

    private final QQEnvironment environment;
    private final User user;
    private final boolean stranger;
    private final boolean group;

    public QQUser(QQEnvironment environment, User user, boolean stranger, boolean group) {
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
        if (!environment.isNudgeEnabled())
            return;
        if (contact instanceof QQUser)
            user.nudge().sendTo(((QQUser) contact).getUser());
        else
            user.nudge().sendTo(((QQGroup) contact).getGroup());
    }

    @Override
    public String getNameInGroup(GroupInfo group) {
        if (group instanceof QQGroup qq) {
            Member member = qq.getGroup().getMembers().get(user.getId());
            String name = member == null ? null : member.getNameCard();
            return name == null || name.isEmpty() ? getName() : name;
        } else
            return null;
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
