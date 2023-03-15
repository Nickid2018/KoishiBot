package io.github.nickid2018.koishibot.message.action;

import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.network.ByteData;
import io.github.nickid2018.koishibot.network.SerializableData;

public class RecallAction implements SerializableData {

    private final Environment env;
    public String messageUniqueID;

    public RecallAction(Environment env) {
        this.env = env;
    }

    @Override
    public void read(ByteData buf) {
        messageUniqueID = buf.readString();
    }

    @Override
    public void write(ByteData buf) {
        buf.writeString(messageUniqueID);
    }
}
