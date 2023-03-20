package io.github.nickid2018.koishibot.message.kook;

import io.github.kookybot.message.SelfMessage;
import io.github.nickid2018.koishibot.message.api.TextMessage;

public class KOOKText extends TextMessage implements KOOKMessage {
    private SelfMessage sentMessage;

    public KOOKText(KOOKEnvironment environment) {
        super(environment);
    }

    public KOOKText(KOOKEnvironment environment, String message) {
        super(environment);
        text = message.replace("[", "\\[").replace("]", "\\]");
    }

    @Override
    public void formatMessage(KOOKMessageData data) {
        data.getTexts().add(text);
    }

    @Override
    public void setSentMessage(SelfMessage message) {
        sentMessage = message;
    }

    @Override
    public SelfMessage getSentMessage() {
        return sentMessage;
    }
}
