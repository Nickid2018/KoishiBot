package io.github.nickid2018.koishibot.message.qq;

import io.github.nickid2018.koishibot.message.api.AtMessage;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.Message;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

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
    public AtMessage fillAt(GroupInfo group, UserInfo contact) {
        at = new At(((QQUser) contact).getUser().getId());
        return this;
    }

    @Nullable
    @Override
    public UserInfo getUser(GroupInfo group) {
        return new QQUser(((QQGroup) group).getGroup().get(at.getTarget()), false, true);
    }

    @NotNull
    @Override
    public String getId() {
        return "qq.user" + at.getTarget();
    }

    @Override
    public Message getQQMessage() {
        return at;
    }
}
