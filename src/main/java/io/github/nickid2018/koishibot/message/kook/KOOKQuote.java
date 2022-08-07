package io.github.nickid2018.koishibot.message.kook;

import io.github.nickid2018.koishibot.message.api.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class KOOKQuote implements QuoteMessage {

    private final KOOKEnvironment environment;
    private KOOKChain message;

    public KOOKQuote(KOOKEnvironment environment) {
        this.environment = environment;
    }

    public KOOKQuote(KOOKEnvironment environment, KOOKChain chain) {
        this.environment = environment;
        message = chain;
    }

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public void recall() {
        // Unsupported, single quote cannot be sent
    }

    @Override
    public long getSentTime() {
        // Unsupported, single quote cannot be sent
        return -1;
    }

    @Override
    public MessageFrom getSource() {
        // Unsupported, single quote cannot be sent
        return null;
    }

    @Override
    public QuoteMessage fill(ChainMessage message) {
        if (message instanceof KOOKChain chain)
            this.message = chain;
        return this;
    }

    @Nullable
    @Override
    public UserInfo getReplyTo() {
        return null;
    }

    @NotNull
    @Override
    public String getReplyToID() {
        return null;
    }

    @Override
    public ChainMessage getQuoteMessage() {
        return message;
    }

    @Override
    public MessageFrom getQuoteFrom() {
        return new KOOKMessageFrom(message.getKOOKMessage().left().content());
    }
}
