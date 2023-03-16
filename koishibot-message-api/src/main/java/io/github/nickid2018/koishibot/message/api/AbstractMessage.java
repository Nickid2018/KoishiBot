package io.github.nickid2018.koishibot.message.api;

import io.github.nickid2018.koishibot.message.action.SendMessageAction;
import io.github.nickid2018.koishibot.network.ByteData;
import io.github.nickid2018.koishibot.network.SerializableData;
import io.github.nickid2018.koishibot.util.Either;

public abstract class AbstractMessage implements SerializableData {

    protected final Environment env;
    protected MessageSource source;

    public AbstractMessage(Environment env) {
        this.env = env;
    }

    public Environment getEnvironment() {
        return env;
    }

    public void send(UserInfo contact) {
        SendMessageAction event = new SendMessageAction(env);
        event.target = Either.left(contact);
        event.message = this;
        env.getConnection().sendPacket(event);
    }

    public void send(GroupInfo group) {
        SendMessageAction event = new SendMessageAction(env);
        event.target = Either.right(group);
        event.message = this;
        env.getConnection().sendPacket(event);
    }

    public void recall() {
        if (source != null)
            source.recall();
    }

    public long getSentTime() {
        if (source != null)
            return source.getSentTime();
        return -1;
    }

    public MessageSource getSource() {
        return source;
    }

    @Override
    public void read(ByteData buf) {
        source = buf.readSerializableDataOrNull(env.getConnection(), MessageSource.class);
        readAdditional(buf);
    }

    @Override
    public void write(ByteData buf) {
        buf.writeSerializableDataOrNull(env.getConnection().getRegistry(), source);
        writeAdditional(buf);
    }

    protected void readAdditional(ByteData buf) {
    }

    protected void writeAdditional(ByteData buf) {
    }
}
