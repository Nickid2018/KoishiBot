package io.github.nickid2018.koishibot.message.query;

import io.github.nickid2018.koishibot.message.api.*;
import io.github.nickid2018.koishibot.network.ByteData;
import io.github.nickid2018.koishibot.network.Connection;
import io.github.nickid2018.koishibot.util.Either;
import io.netty.buffer.Unpooled;

public class SendMessageQuery extends Query {

    private final Environment env;

    public AbstractMessage message;
    public Either<UserInfo, GroupInfo> target;

    public SendMessageQuery(Environment env) {
        this.env = env;
    }

    @Override
    public void read(ByteData buf) {
        super.read(buf);
        message = (AbstractMessage) buf.readSerializableData(env.getConnection());
        if (buf.readBoolean())
            target = Either.left(buf.readSerializableData(env.getConnection(), UserInfo.class));
        else
            target = Either.right(buf.readSerializableData(env.getConnection(), GroupInfo.class));
    }

    @Override
    public void write(ByteData buf) {
        super.write(buf);
        buf.writeSerializableDataMultiChoice(env.getConnection().getRegistry(), message);
        if (target.isLeft()) {
            buf.writeBoolean(true);
            buf.writeSerializableData(target.left());
        } else {
            buf.writeBoolean(false);
            buf.writeSerializableData(target.right());
        }
    }

    public static byte[] toBytes(Connection connection, MessageSource source) {
        ByteData buf = new ByteData(Unpooled.buffer());
        buf.writeSerializableDataOrNull(connection.getRegistry(), source);
        byte[] data = buf.toByteArray();
        buf.release();
        return data;
    }

    public static MessageSource fromBytes(Connection connection, byte[] data) {
        if (data == null)
            return null;
        ByteData buf = new ByteData(Unpooled.wrappedBuffer(data));
        MessageSource source = buf.readSerializableDataOrNull(connection, MessageSource.class);
        buf.release();
        return source;
    }
}
