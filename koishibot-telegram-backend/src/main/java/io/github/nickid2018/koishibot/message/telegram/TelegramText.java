package io.github.nickid2018.koishibot.message.telegram;

import io.github.nickid2018.koishibot.message.api.TextMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

public class TelegramText extends TextMessage implements TelegramMessage {

    private Message sentMessage;

    public TelegramText(TelegramEnvironment environment) {
        super(environment);
    }

    public TelegramText(TelegramEnvironment environment, String text) {
        super(environment);
        this.text = text;
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
        data.getTexts().add(text);
    }
}
