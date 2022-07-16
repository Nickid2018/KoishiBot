package io.github.nickid2018.koishibot.message.qq;

import io.github.nickid2018.koishibot.message.api.ContactInfo;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import net.mamoe.mirai.contact.Group;

public class QQGroup implements GroupInfo {

    private final Environment environment;
    private final Group group;

    public QQGroup(Environment environment, Group group) {
        this.environment = environment;
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
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public boolean equals(ContactInfo info) {
        if (!(info instanceof QQGroup))
            return false;
        return group.getId() == ((QQGroup) info).group.getId();
    }

    @Override
    public String getName() {
        return group.getName();
    }
}
