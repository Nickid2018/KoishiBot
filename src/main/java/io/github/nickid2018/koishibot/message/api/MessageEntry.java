package io.github.nickid2018.koishibot.message.api;

public interface MessageEntry {

    MessageEntry fill(String id, String name, AbstractMessage message, int time);
}
