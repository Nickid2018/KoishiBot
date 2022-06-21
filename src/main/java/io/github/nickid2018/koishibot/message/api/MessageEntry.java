package io.github.nickid2018.koishibot.message.api;

public interface MessageEntry {

    MessageEntry fillMessageEntry(String id, String name, AbstractMessage message, int time);
}
