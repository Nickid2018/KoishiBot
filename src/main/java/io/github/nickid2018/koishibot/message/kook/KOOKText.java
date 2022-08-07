package io.github.nickid2018.koishibot.message.kook;

import io.github.kookybot.message.MarkdownMessage;
import io.github.kookybot.message.Message;
import io.github.kookybot.message.MessageComponent;
import io.github.nickid2018.koishibot.message.api.TextMessage;
import io.github.nickid2018.koishibot.util.value.Either;

public class KOOKText extends KOOKMessage implements TextMessage {

    private MarkdownMessage message;

    public KOOKText(KOOKEnvironment environment) {
        super(environment);
    }

    public KOOKText(KOOKEnvironment environment, MarkdownMessage message) {
        super(environment);
        this.message = message;
    }

    @Override
    public TextMessage fillText(String text) {
        message = new MarkdownMessage(environment.getKookClient(), text);
        return this;
    }

    @Override
    public String getText() {
        return message.content();
    }

    @Override
    public Either<Message, MessageComponent> getKOOKMessage() {
        return Either.left(message);
    }
}
