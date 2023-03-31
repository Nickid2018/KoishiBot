package io.github.nickid2018.koishibot.message.qq;

import io.github.nickid2018.koishibot.message.api.ContactInfo;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import io.github.nickid2018.koishibot.network.ByteData;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.contact.Stranger;
import net.mamoe.mirai.contact.User;

public class QQUser extends UserInfo {
    private User user;

    public QQUser(QQEnvironment environment) {
        super(environment);
    }

    public QQUser(QQEnvironment environment, User user, boolean stranger) {
        super(environment);
        isStranger = stranger || user instanceof Stranger;

        if (user != null) {
            userId = "qq.user" + user.getId();
            name = user.getNick();
        } else {
            userId = "qq.user-anonymous";
            name = "Anonymous";
        }

        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public static void nudge(QQUser user, ContactInfo contact) {
        if (!((QQEnvironment) user.env).isNudgeEnabled())
            return;
        if (user.user == null)
            return;
        if (contact instanceof QQUser)
            user.user.nudge().sendTo(((QQUser) contact).getUser());
        else
            user.user.nudge().sendTo(((QQGroup) contact).getGroup());
    }

    public static String getNameInGroup(QQUser user, GroupInfo group) {
        if (user.user == null)
            return user.name;
        if (group instanceof QQGroup qq) {
            Member member = qq.getGroup().getMembers().get(user.user.getId());
            String name = member == null ? null : member.getNameCard();
            return name == null || name.isEmpty() ? user.name : name;
        } else
            return user.user.getNick();
    }

    @Override
    public void read(ByteData buf) {
        super.read(buf);
        if (userId.equals("qq.user-anonymous"))
            return;
        user = ((QQEnvironment) env).getQQUser(userId, isStranger, true);
    }
}
