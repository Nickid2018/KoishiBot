package io.github.nickid2018.koishibot.message.qq;

import io.github.nickid2018.koishibot.message.api.AbstractMessage;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.QuoteMessage;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.QuoteReply;

public class QQQuote extends QQMessage implements QuoteMessage {

    private QuoteReply quoteReply;

    protected QQQuote(QQEnvironment environment) {
        super(environment);
    }

    protected QQQuote(QQEnvironment environment, QuoteReply quoteReply) {
        super(environment);
        this.quoteReply = quoteReply;
    }

    @Override
    protected Message getQQMessage() {
        return quoteReply;
    }

    @Override
    public QuoteMessage fill(AbstractMessage message) {
        quoteReply = new QuoteReply((MessageChain) ((QQChain) message).getQQMessage());
        return this;
    }

    @Override
    public UserInfo getReplyTo() {
        return environment.getUser("qq.user" + quoteReply.component1().getFromId(), true);
    }

    @Override
    public void send(GroupInfo group) {
        QuoteMessage.super.send(group);
    }

    @Override
    public void send(UserInfo user) {
        QuoteMessage.super.send(user);
    }
}
