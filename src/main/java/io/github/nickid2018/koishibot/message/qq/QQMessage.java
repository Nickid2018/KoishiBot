package io.github.nickid2018.koishibot.message.qq;

import io.github.nickid2018.koishibot.message.api.AbstractMessage;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import net.mamoe.mirai.message.MessageReceipt;
import net.mamoe.mirai.message.data.Message;

public abstract class QQMessage implements AbstractMessage {

    protected MessageReceipt<?> receipt;
    protected final QQEnvironment environment;

    protected QQMessage(QQEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public void send(GroupInfo group) {
        QQGroup qq = (QQGroup) group;
        receipt = qq.getGroup().sendMessage(getQQMessage());
    }

    @Override
    public void send(UserInfo user) {
        QQUser qq = (QQUser) user;
        receipt = qq.getUser().sendMessage(getQQMessage());
    }

    @Override
    public void recall() {
        if (receipt != null)
            receipt.recall();
    }

    @Override
    public long getSentTime() {
        return receipt.getSource().getTime();
    }

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    protected abstract Message getQQMessage();

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof QQMessage))
            return false;
        return getQQMessage().equals(((QQMessage) o).getQQMessage());
    }
}
