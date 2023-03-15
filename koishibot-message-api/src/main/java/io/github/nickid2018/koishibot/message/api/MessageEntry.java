package io.github.nickid2018.koishibot.message.api;

import io.github.nickid2018.koishibot.network.ByteData;
import io.github.nickid2018.koishibot.network.SerializableData;

public class MessageEntry implements SerializableData {

    private final Environment env;
    public String id;
    public String name;
    public AbstractMessage message;
    public int time;

    public MessageEntry(Environment env) {
        this.env = env;
    }

    public Environment getEnv() {
        return env;
    }

    public MessageEntry fillMessageEntry(String id, String name, AbstractMessage message, int time) {
        this.id = id;
        this.name = name;
        this.message = message;
        this.time = time;
        return this;
    }

    @Override
    public void read(ByteData buf) {
        id = buf.readString();
        name = buf.readString();
        message = (AbstractMessage) buf.readSerializableData(env.getConnection().getRegistry());
        time = buf.readInt();
    }

    @Override
    public void write(ByteData buf) {
        buf.writeString(id);
        buf.writeString(name);
        buf.writeSerializableDataMultiChoice(env.getConnection().getRegistry(), message);
        buf.writeInt(time);
    }
}
