package io.github.nickid2018.koishibot.message.kook;

import io.github.nickid2018.koishibot.message.api.QuoteMessage;

public class KOOKQuote extends QuoteMessage {

    private KOOKChain chainMessage;

    public KOOKQuote(KOOKEnvironment environment) {
        super(environment);
    }

    public KOOKQuote(KOOKEnvironment environment, KOOKChain chain) {
        super(environment);
        message = new KOOKChain((KOOKEnvironment) env, chain.getData().getQuoteData());
        replyTo = new KOOKUser(env, chain.getData().getQuoteUser());
        quoteFrom = new KOOKMessageSource(env, chain.getData().getQuoteID(), null);
        chainMessage = chain;
    }

    public KOOKChain getMessage() {
        return chainMessage;
    }
}
