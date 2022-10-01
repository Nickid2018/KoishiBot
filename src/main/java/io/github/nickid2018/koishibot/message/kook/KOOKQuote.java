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
        return message.getSource();
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
        return new KOOKUser(environment, message.getData().getQuoteUser());
    }

    @NotNull
    @Override
    public String getReplyToID() {
        return "kook.user" + message.getData().getQuoteUser().getId();
    }

    @Override
    public ChainMessage getQuoteMessage() {
        return new KOOKChain(environment, message.getData().getQuoteData());
    }

    @Override
    public MessageFrom getQuoteFrom() {
        return new KOOKMessageFrom(message.getData().getQuoteID());
    }

    public KOOKChain getMessage() {
        return message;
    }
}
