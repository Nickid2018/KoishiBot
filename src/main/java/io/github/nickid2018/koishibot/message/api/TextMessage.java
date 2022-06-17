package io.github.nickid2018.koishibot.message.api;

public interface TextMessage extends AbstractMessage {

    TextMessage fillText(String text);

    String getText();
}
