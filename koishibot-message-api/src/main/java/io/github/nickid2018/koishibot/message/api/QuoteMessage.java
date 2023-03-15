package io.github.nickid2018.koishibot.message.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class QuoteMessage extends AbstractMessage {

    protected ChainMessage message;
    protected MessageSource quoteFrom;
    protected UserInfo replyTo;
    protected String replyToID;

    public QuoteMessage(Environment env) {
        super(env);
    }

    public QuoteMessage fill(ChainMessage message) {
        this.message = message;
        this.quoteFrom = message.source;
        return this;
    }

    @Nullable
    public UserInfo getReplyTo() {
        return replyTo;
    }

    @Nonnull
    public String getReplyToID() {
        return replyTo.getUserId();
    }

    public ChainMessage getQuoteMessage() {
        return message;
    }

    public MessageSource getQuoteFrom() {
        return quoteFrom;
    }

    public void send(GroupInfo group) {
        format().send(group);
    }

    public void send(UserInfo contact) {
        format().send(contact);
    }

    public ChainMessage format() {
        return env.newChain(this, env.newText(" "));
    }
}
