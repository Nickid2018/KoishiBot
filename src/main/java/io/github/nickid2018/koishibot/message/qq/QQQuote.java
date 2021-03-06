package io.github.nickid2018.koishibot.message.qq;

import io.github.nickid2018.koishibot.message.api.*;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.QuoteReply;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

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
    public Message getQQMessage() {
        return quoteReply;
    }

    @Override
    public QuoteMessage fill(ChainMessage message) {
        quoteReply = new QuoteReply((MessageChain) ((QQChain) message).getQQMessage());
        return this;
    }

    @Nullable
    @Override
    public UserInfo getReplyTo() {
        return environment.getUser("qq.user" + quoteReply.getSource().getFromId(), true);
    }

    @NotNull
    @Override
    public String getReplyToID() {
        return "qq.user" + quoteReply.getSource().getFromId();
    }

    @Override
    public ChainMessage getQuoteMessage() {
        return new QQChain(environment, quoteReply.getSource().getOriginalMessage());
    }

    @Override
    public MessageFrom getQuoteFrom() {
        return new QQMessageFrom(quoteReply.getSource());
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
