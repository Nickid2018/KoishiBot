package io.github.nickid2018.koishibot.message.qq;

import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.network.ByteData;
import net.mamoe.mirai.contact.Group;

public class QQGroup extends GroupInfo {

    private Group group;

    public QQGroup(QQEnvironment environment) {
        super(environment);
    }

    public QQGroup(QQEnvironment environment, Group group) {
        super(environment);

        groupId = "qq.group" + group.getId();
        name = group.getName();

        this.group = group;
    }

    public Group getGroup() {
        return group;
    }

    @Override
    public void read(ByteData buf) {
        super.read(buf);
        group = ((QQEnvironment) env).getQQGroup(groupId);
    }
}
