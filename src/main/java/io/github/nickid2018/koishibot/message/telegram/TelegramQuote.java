package io.github.nickid2018.koishibot.message.telegram;

import io.github.nickid2018.koishibot.message.api.ChainMessage;
import io.github.nickid2018.koishibot.message.api.MessageFrom;
import io.github.nickid2018.koishibot.message.api.QuoteMessage;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.telegram.telegrambots.meta.api.objects.User;

public class TelegramQuote extends TelegramMessage implements QuoteMessage {

    private User user;
    private TelegramMessageData quoteData;

    public TelegramQuote(TelegramEnvironment environment) {
        super(environment);
        quoteData = new TelegramMessageData();
    }

    public TelegramQuote(TelegramEnvironment environment, TelegramMessageData data, User user) {
        super(environment);
        quoteData = data;
        this.user = user;
    }

    @Override
    public QuoteMessage fill(ChainMessage message) {
        if (message instanceof TelegramChain chain) {
            quoteData = chain.getData();
            user = chain.getData().getSender();
        }
        return this;
    }

    @Nullable
    @Override
    public UserInfo getReplyTo() {
        return new TelegramUser(environment, user);
    }

    @NotNull
    @Override
    public String getReplyToID() {
        return "tg.user" + user.getId();
    }

    @Override
    public ChainMessage getQuoteMessage() {
        return new TelegramChain(environment, quoteData);
    }

    @Override
    public MessageFrom getQuoteFrom() {
        return new TelegramMessageFrom(environment, quoteData.getChatID(), quoteData.getMsgID());
    }

    @Override
    protected void formatMessage(TelegramMessageData data) {
        data.setQuoteID(quoteData.getMsgID());
        data.setQuoteUser(user);
        data.setQuoteMsg(quoteData);
    }
}
