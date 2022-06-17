package io.github.nickid2018.koishibot.message.qq;

import io.github.nickid2018.koishibot.message.api.ContactInfo;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import net.mamoe.mirai.contact.Group;

public class QQGroup implements GroupInfo {

    private final Group group;

    public QQGroup(Group group) {
        this.group = group;
    }

    @Override
    public String getGroupId() {
        return "qq.group" + group.getId();
    }

    public Group getGroup() {
        return group;
    }

    @Override
    public boolean equals(ContactInfo info) {
        if (!(info instanceof QQGroup))
            return false;
        return group.getId() == ((QQGroup) info).group.getId();
    }
}
