package io.github.nickid2018.koishibot.message.telegram;

import io.github.nickid2018.koishibot.message.api.TextMessage;

public class TelegramText extends TelegramMessage implements TextMessage {

    private String text;

    public TelegramText(TelegramEnvironment environment) {
        super(environment);
    }

    public TelegramText(TelegramEnvironment environment, String text) {
        super(environment);
        this.text = text;
    }

    @Override
    public void formatMessage(TelegramMessageData data) {
        data.getTexts().add(text);
    }

    @Override
    public TextMessage fillText(String text) {
        this.text = text;
        return this;
    }

    @Override
    public String getText() {
        return text;
    }
}
