package io.github.nickid2018.koishibot.message.qq;

import io.github.nickid2018.koishibot.message.api.AtMessage;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.Message;

public class QQAt extends QQMessage implements AtMessage {

    private At at;

    protected QQAt(QQEnvironment environment) {
        super(environment);
    }

    protected QQAt(QQEnvironment environment, At at) {
        super(environment);
        this.at = at;
    }

    @Override
    public AtMessage fill(UserInfo contact) {
        at = new At(((QQUser) contact).getUser().getId());
        return this;
    }

    @Override
    public UserInfo getUser(GroupInfo group) {
        return new QQUser(((QQGroup) group).getGroup().get(at.component1()), false, true);
    }

    @Override
    protected Message getQQMessage() {
        return at;
    }
}
