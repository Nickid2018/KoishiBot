package io.github.nickid2018.koishibot.message.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface QuoteMessage extends AbstractMessage {

    QuoteMessage fill(ChainMessage message);

    @Nullable
    UserInfo getReplyTo();

    @Nonnull
    String getReplyToID();

    ChainMessage getQuoteMessage();

    MessageFrom getQuoteFrom();

    default void send(GroupInfo group) {
        format().send(group);
    }

    default void send(UserInfo contact) {
        format().send(contact);
    }

    default ChainMessage format() {
        Environment environment = getEnvironment();
        return environment.chain().fillChain(this, environment.text().fillText(" "));
    }
}
