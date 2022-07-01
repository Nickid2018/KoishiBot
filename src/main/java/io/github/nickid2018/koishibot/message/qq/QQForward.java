package io.github.nickid2018.koishibot.message.qq;

import io.github.nickid2018.koishibot.message.api.ContactInfo;
import io.github.nickid2018.koishibot.message.api.ForwardMessage;
import io.github.nickid2018.koishibot.message.api.MessageEntry;
import net.mamoe.mirai.message.data.ForwardMessageBuilder;
import net.mamoe.mirai.message.data.Message;

public class QQForward extends QQMessage implements ForwardMessage {

    private net.mamoe.mirai.message.data.ForwardMessage forward;

    protected QQForward(QQEnvironment environment) {
        super(environment);
    }

    protected QQForward(QQEnvironment environment, net.mamoe.mirai.message.data.ForwardMessage forward) {
        super(environment);
        this.forward = forward;
    }

    @Override
    public ForwardMessage fillForwards(ContactInfo contact, MessageEntry... entries) {
        ForwardMessageBuilder builder = new ForwardMessageBuilder(
                contact instanceof QQGroup ? ((QQGroup) contact).getGroup() : ((QQUser) contact).getUser());
        for (MessageEntry entry : entries) {
            QQMessageEntry qq = (QQMessageEntry) entry;
            builder.add(qq.id, qq.name, qq.message, qq.time);
        }
        forward = builder.build();
        return this;
    }

    @Override
    protected Message getQQMessage() {
        return forward;
    }
}
