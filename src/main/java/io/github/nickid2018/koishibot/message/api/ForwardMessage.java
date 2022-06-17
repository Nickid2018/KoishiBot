package io.github.nickid2018.koishibot.message.api;

public interface ForwardMessage extends AbstractMessage {

    ForwardMessage fill(ContactInfo group, MessageEntry... entries);
}
