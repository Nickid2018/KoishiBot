package io.github.nickid2018.koishibot.message.telegram;

import io.github.nickid2018.koishibot.message.api.ChainMessage;
import io.github.nickid2018.koishibot.message.api.QuoteMessage;
import io.github.nickid2018.koishibot.network.ByteData;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

public class TelegramQuote extends QuoteMessage implements TelegramMessage {

    private User user;
    private TelegramMessageData quoteData;

    public TelegramQuote(TelegramEnvironment environment) {
        super(environment);
    }

    public TelegramQuote(TelegramEnvironment environment, TelegramMessageData data, User user) {
        super(environment);
        quoteData = data;
        this.user = user;
        message = new TelegramChain((TelegramEnvironment) env, data);
        replyTo = new TelegramUser((TelegramEnvironment) env, user);
        quoteFrom = message.getSource();
    }

    @Override
    public void read(ByteData buf) {
        super.read(buf);
        if (message instanceof TelegramChain chain) {
            quoteData = chain.getData();
            user = chain.getData().getSender();
        }
    }

    @Override
    public ChainMessage getQuoteMessage() {
        return new TelegramChain((TelegramEnvironment) env, quoteData);
    }

    @Override
    public void setSentMessage(Message message) {
    }

    @Override
    public Message getSentMessage() {
        return null;
    }

    @Override
    public void formatMessage(TelegramMessageData data) {
        data.setQuoteID(quoteData.getMsgID());
        data.setQuoteUser(user);
        data.setQuoteMsg(quoteData);
    }
}
