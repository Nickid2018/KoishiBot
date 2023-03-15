package io.github.nickid2018.koishibot.message.qq;

import io.github.nickid2018.koishibot.message.api.TextMessage;
import io.github.nickid2018.koishibot.network.ByteData;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.PlainText;

public class QQText extends TextMessage implements QQMessage {

    private PlainText textMessage;

    protected QQText(QQEnvironment environment) {
        super(environment);
    }

    protected QQText(QQEnvironment environment, PlainText textMessage) {
        super(environment);
        this.textMessage = textMessage;
        this.text = textMessage.getContent();
    }

    @Override
    public Message getMessage() {
        return textMessage;
    }

    @Override
    public void read(ByteData buf) {
        super.read(buf);
        textMessage = new PlainText(text);
    }
}
