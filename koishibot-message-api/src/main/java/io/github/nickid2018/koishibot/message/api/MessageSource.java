package io.github.nickid2018.koishibot.message.api;

import io.github.nickid2018.koishibot.message.action.RecallEvent;
import io.github.nickid2018.koishibot.network.ByteData;
import io.github.nickid2018.koishibot.network.SerializableData;

public class MessageSource implements SerializableData {

    private final Environment env;
    private String messageUniqueID;
    private long sentTime;

    public MessageSource(Environment env) {
        this.env = env;
    }

    public boolean equals(MessageSource source) {
        return messageUniqueID.equals(source.messageUniqueID);
    }

    public void recall() {
        RecallEvent event = new RecallEvent(env);
        event.messageUniqueID = messageUniqueID;
        env.getConnection().sendPacket(event);
    }

    @Override
    public void read(ByteData buf) {
        messageUniqueID = buf.readString();
        sentTime = buf.readLong();
    }

    @Override
    public void write(ByteData buf) {
        buf.writeString(messageUniqueID);
        buf.writeLong(sentTime);
    }

    public long getSentTime() {
        return sentTime;
    }

    public String getMessageUniqueID() {
        return messageUniqueID;
    }
}
