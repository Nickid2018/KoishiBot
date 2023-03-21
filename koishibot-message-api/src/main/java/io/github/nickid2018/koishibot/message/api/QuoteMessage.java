package io.github.nickid2018.koishibot.message.api;

import io.github.nickid2018.koishibot.network.ByteData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class QuoteMessage extends AbstractMessage {

    protected ChainMessage message;
    protected MessageSource quoteFrom;
    protected UserInfo replyTo;

    public QuoteMessage(Environment env) {
        super(env);
    }

    public QuoteMessage fill(ChainMessage message) {
        this.message = message;
        this.quoteFrom = message.source;
        this.replyTo = null;
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

    @Override
    protected void readAdditional(ByteData buf) {
        message = (ChainMessage) buf.readSerializableData(env.getConnection());
        quoteFrom = buf.readSerializableData(env.getConnection(), MessageSource.class);
        replyTo = buf.readSerializableDataOrNull(env.getConnection(), UserInfo.class);
    }

    @Override
    protected void writeAdditional(ByteData buf) {
        buf.writeSerializableDataMultiChoice(env.getConnection().getRegistry(), message);
        buf.writeSerializableData(quoteFrom);
        buf.writeSerializableDataOrNull(env.getConnection().getRegistry(), replyTo);
    }
}
