package io.github.nickid2018.koishibot.message.qq;

import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.TextMessage;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.PlainText;

public class QQText extends QQMessage implements TextMessage {

    private PlainText text;

    protected QQText(QQEnvironment environment) {
        super(environment);
    }

    protected QQText(QQEnvironment environment, PlainText text) {
        super(environment);
        this.text = text;
    }

    @Override
    public TextMessage fillText(String text) {
        this.text = new PlainText(text);
        return this;
    }

    @Override
    public String getText() {
        return text.component1();
    }

    @Override
    protected Message getQQMessage() {
        return text;
    }
}
