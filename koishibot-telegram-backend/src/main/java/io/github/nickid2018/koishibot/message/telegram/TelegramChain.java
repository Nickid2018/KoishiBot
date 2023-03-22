package io.github.nickid2018.koishibot.message.telegram;

import io.github.nickid2018.koishibot.message.api.AbstractMessage;
import io.github.nickid2018.koishibot.message.api.ChainMessage;
import io.github.nickid2018.koishibot.network.ByteData;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;

import java.util.ArrayList;
import java.util.List;

public class TelegramChain extends ChainMessage implements TelegramMessage {

    private TelegramMessageData data;
    private Message sentMessage;

    public TelegramChain(TelegramEnvironment environment) {
        super(environment);
    }

    public TelegramChain(TelegramEnvironment environment, TelegramMessageData data) {
        super(environment);
        this.data = data;
        this.source = new TelegramMessageSource(environment, data.getChatID(), data.getMsgID());
        messages = getMessages();
    }

    @Override
    public void read(ByteData buf) {
        super.read(buf);
        data = new TelegramMessageData();
        for (AbstractMessage message : messages)
            if (message instanceof TelegramMessage telegramMessage)
                telegramMessage.formatMessage(data);
    }

    @Override
    public AbstractMessage[] getMessages() {
        List<AbstractMessage> messages = new ArrayList<>();
        if (data.getQuoteID() != -1)
            messages.add(new TelegramQuote((TelegramEnvironment) env, data.getQuoteMsg(), data.getQuoteUser()));
        for (String text : data.getTexts())
            messages.add(new TelegramText((TelegramEnvironment) env, text));
        for (MessageEntity entity : data.getMentionUsers())
            messages.add(new TelegramAt((TelegramEnvironment) env, entity.getUser()));
        if (data.getAudio() != null)
            messages.add(new TelegramAudio((TelegramEnvironment) env, data.getAudio()));
        if (data.getImage() != null) {
            try {
                messages.add(new TelegramImage((TelegramEnvironment) env, data.getImage()));
            } catch (Exception e) {
                TelegramEnvironment.LOGGER.error("Failed to convert image", e);
            }
        }
        return messages.toArray(AbstractMessage[]::new);
    }

    @Override
    public void setSentMessage(Message message) {
        sentMessage = message;
        source = new TelegramMessageSource((TelegramEnvironment) env, message.getChatId(), message.getMessageId());
    }

    @Override
    public Message getSentMessage() {
        return sentMessage;
    }

    @Override
    public void formatMessage(TelegramMessageData data) {
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
