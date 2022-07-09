package io.github.nickid2018.koishibot.message.qq;

import io.github.nickid2018.koishibot.message.api.*;
import net.mamoe.mirai.message.MessageReceipt;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageSource;

import java.util.Arrays;

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

    @Override
    public MessageFrom getSource() {
        return receipt == null ? null : new QQMessageFrom(receipt.getSource());
    }

    public abstract Message getQQMessage();

    public MessageReceipt<?> getReceipt() {
        return receipt;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof QQMessage))
            return false;
        Message other = ((QQMessage) o).getQQMessage();
        if (other instanceof MessageChain) {
            MessageChain chain = (MessageChain) other;
            MessageSource source = null;
            if (receipt != null && (source = chain.get(MessageSource.Key)) != null)
                if (Arrays.equals(source.getIds(), receipt.getSource().getIds()))
                    return true;
            Message thisMessage = getQQMessage();
            if (thisMessage instanceof MessageChain) {
                MessageChain thisChain = (MessageChain) thisMessage;
                MessageSource thisSource;
                if (source != null && (thisSource = thisChain.get(MessageSource.Key)) != null)
                    if (Arrays.equals(source.getIds(), thisSource.getIds()))
                        return true;
            }
        }
        return getQQMessage().equals(other);
    }
}
