package io.github.nickid2018.koishibot.message.kook;

import io.github.nickid2018.koishibot.message.api.TextMessage;

public class KOOKText extends KOOKMessage implements TextMessage {

    private String message;

    public KOOKText(KOOKEnvironment environment) {
        super(environment);
    }

    public KOOKText(KOOKEnvironment environment, String message) {
        super(environment);
        this.message = message.replace("[", "\\[").replace("]", "\\]");
    }

    @Override
    public TextMessage fillText(String text) {
        message = text;
        return this;
    }

    @Override
    public String getText() {
        return message.replace("\\[", "[").replace("\\]", "]");
    }

    @Override
    public void formatMessage(KOOKMessageData data) {
        data.getTexts().add(message);
    }
}
