package io.github.nickid2018.koishibot.message.qq;

import io.github.nickid2018.koishibot.message.api.AbstractMessage;
import io.github.nickid2018.koishibot.message.api.MessageEntry;
import net.mamoe.mirai.message.data.Message;

public class QQMessageEntry implements MessageEntry {

    private final QQEnvironment environment;

    public long id;
    public String name;
    public Message message;
    public int time;

    public QQMessageEntry(QQEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public MessageEntry fillMessageEntry(String id, String name, AbstractMessage message, int time) {
        this.id = Long.parseLong(id.substring(7));
        this.name = name;
        this.message = ((QQMessage) message).getQQMessage();
        this.time = time;
        return this;
    }
}
