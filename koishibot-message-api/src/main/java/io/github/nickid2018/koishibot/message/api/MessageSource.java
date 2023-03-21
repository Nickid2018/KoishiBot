package io.github.nickid2018.koishibot.message.api;

import io.github.nickid2018.koishibot.message.action.RecallAction;
import io.github.nickid2018.koishibot.network.ByteData;
import io.github.nickid2018.koishibot.network.SerializableData;
import io.github.nickid2018.koishibot.network.StringData;

public class MessageSource implements SerializableData {

    protected final Environment env;
    protected String messageUniqueID;
    protected long sentTime;

    public MessageSource(Environment env) {
        this.env = env;
    }

    public boolean equals(MessageSource source) {
        return messageUniqueID != null && source.messageUniqueID != null && messageUniqueID.equals(source.messageUniqueID);
    }

    public void recall() {
        if (messageUniqueID == null)
            return;
        RecallAction event = new RecallAction(env);
        event.messageUniqueID = messageUniqueID;
        env.getConnection().sendPacket(event);
    }

    @Override
    public void read(ByteData buf) {
        StringData data = buf.readSerializableDataOrNull(env.getConnection(), StringData.class);
        if (data != null)
            messageUniqueID = data.getStr();
        sentTime = buf.readLong();
    }

    @Override
    public void write(ByteData buf) {
        StringData data = messageUniqueID == null ? null : new StringData(messageUniqueID);
        buf.writeSerializableDataOrNull(env.getConnection().getRegistry(), data);
        buf.writeLong(sentTime);
    }

    public long getSentTime() {
        return sentTime;
    }

    public String getMessageUniqueID() {
        return messageUniqueID;
    }
}
