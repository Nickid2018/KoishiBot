package io.github.nickid2018.koishibot.message.api;

public interface ForwardMessage extends AbstractMessage {

    ForwardMessage fillForwards(ContactInfo group, MessageEntry... entries);
}
