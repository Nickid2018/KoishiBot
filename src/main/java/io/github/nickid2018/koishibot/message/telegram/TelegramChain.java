package io.github.nickid2018.koishibot.message.telegram;

import io.github.nickid2018.koishibot.message.api.AbstractMessage;
import io.github.nickid2018.koishibot.message.api.ChainMessage;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;

import java.util.ArrayList;
import java.util.List;

public class TelegramChain extends TelegramMessage implements ChainMessage {

    private TelegramMessageData data;


    public TelegramChain(TelegramEnvironment environment) {
        super(environment);
    }

    public TelegramChain(TelegramEnvironment environment, TelegramMessageData data) {
        super(environment);
        this.data = data;
    }

    @Override
    public ChainMessage fillChain(AbstractMessage... messages) {
        data = new TelegramMessageData();
        for (AbstractMessage message : messages)
            if (message instanceof TelegramMessage telegramMessage)
                telegramMessage.formatMessage(data);
        return this;
    }

    @Override
    public AbstractMessage[] getMessages() {
        List<TelegramMessage> messages = new ArrayList<>();
        if (data.getQuoteID() != -1)
            messages.add(new TelegramQuote(environment, data.getQuoteMsg(), data.getQuoteUser()));
        for (String text : data.getTexts())
            messages.add(new TelegramText(environment, text));
        for (MessageEntity entity : data.getMentionUsers())
            messages.add(new TelegramAt(environment, entity.getUser()));
        if (data.getAudio() != null)
            messages.add(new TelegramAudio(environment, data.getAudio()));
        if (data.getImage() != null)
            messages.add(new TelegramImage(environment, data.getImage()));
        return messages.toArray(AbstractMessage[]::new);
    }

    @Override
    protected void formatMessage(TelegramMessageData data) {
        int addOffset = data.getTexts().stream().mapToInt(String::length).sum();
        this.data.getMentionUsers().forEach(e -> e.setOffset(e.getOffset() + addOffset));
        data.getTexts().addAll(this.data.getTexts());
        data.getMentionUsers().addAll(this.data.getMentionUsers());
        data.setQuoteID(this.data.getQuoteID());
        data.setImageSend(this.data.getImageSend());
        data.setAudioSend(this.data.getAudioSend());
    }

    public TelegramMessageData getData() {
        return data;
    }
}
