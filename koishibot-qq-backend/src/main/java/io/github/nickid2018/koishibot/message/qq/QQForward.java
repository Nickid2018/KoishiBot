package io.github.nickid2018.koishibot.message.qq;

import io.github.nickid2018.koishibot.message.api.ForwardMessage;
import io.github.nickid2018.koishibot.message.api.MessageEntry;
import io.github.nickid2018.koishibot.network.ByteData;
import net.mamoe.mirai.message.data.ForwardMessageBuilder;

public class QQForward extends ForwardMessage implements QQMessage {

    private net.mamoe.mirai.message.data.ForwardMessage forward;

    protected QQForward(QQEnvironment environment) {
        super(environment);
    }

    protected QQForward(QQEnvironment environment, net.mamoe.mirai.message.data.ForwardMessage forward) {
        super(environment);

        this.forward = forward;
        this.entries = forward.getNodeList().stream().map(entry -> {
            QQMessageEntry qq = new QQMessageEntry(environment);
            qq.id = "qq.user" + entry.getSenderId();
            qq.name = entry.getSenderName();
            qq.time = entry.getTime();
            qq.message = new QQChain(environment, entry.getMessageChain());
            return qq;
        }).toArray(MessageEntry[]::new);
    }

    @Override
    public net.mamoe.mirai.message.data.ForwardMessage getMessage() {
        return forward;
    }

    @Override
    public void read(ByteData buf) {
        super.read(buf);
        ForwardMessageBuilder builder = new ForwardMessageBuilder(((QQEnvironment) env).getBot().getAsFriend());
        for (MessageEntry entry : entries) {
            QQMessageEntry qq = (QQMessageEntry) entry;
            builder.add(Long.parseLong(qq.id.substring(7)), qq.name, ((QQMessage) qq).getMessage(), qq.time);
        }
        forward = builder.build();
    }
}
