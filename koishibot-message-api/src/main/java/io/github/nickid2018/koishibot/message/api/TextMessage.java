package io.github.nickid2018.koishibot.message.api;

import io.github.nickid2018.koishibot.network.ByteData;

public class TextMessage extends AbstractMessage {

    private String text;

    public TextMessage(Environment env) {
        super(env);
    }

    public TextMessage fillText(String text) {
        this.text = text;
        return this;
    }

    public String getText() {
        return text;
    }

    @Override
    protected void readAdditional(ByteData buf) {
        text = buf.readString();
    }

    @Override
    protected void writeAdditional(ByteData buf) {
        buf.writeString(text);
    }
}
