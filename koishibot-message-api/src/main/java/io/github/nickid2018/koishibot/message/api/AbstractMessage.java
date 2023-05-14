package io.github.nickid2018.koishibot.message.api;

import io.github.nickid2018.koishibot.message.network.DataPacketListener;
import io.github.nickid2018.koishibot.message.query.SendMessageQuery;
import io.github.nickid2018.koishibot.network.ByteData;
import io.github.nickid2018.koishibot.network.SerializableData;
import io.github.nickid2018.koishibot.util.Either;
import io.github.nickid2018.koishibot.util.LogUtils;

import java.util.concurrent.TimeUnit;

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
        sendInternal(Either.left(contact));
    }

    public void send(GroupInfo group) {
        sendInternal(Either.right(group));
    }

    private void sendInternal(Either<UserInfo, GroupInfo> target) {
        SendMessageQuery sendQuery = new SendMessageQuery(env);
        sendQuery.target = target;
        sendQuery.message = this;
        try {
            byte[] data = env.getListener().queryData(env.getConnection(), sendQuery).get(20, TimeUnit.SECONDS);
            source = SendMessageQuery.fromBytes(env.getConnection(), data);
        } catch (Exception e) {
            LogUtils.error(DataPacketListener.LOGGER, "Failed to send message", e);
        }
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

    public void setMessageSource(MessageSource source) {
        this.source = source;
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
