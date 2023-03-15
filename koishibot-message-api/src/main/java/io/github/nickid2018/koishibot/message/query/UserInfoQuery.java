package io.github.nickid2018.koishibot.message.query;

import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import io.github.nickid2018.koishibot.network.ByteData;
import io.github.nickid2018.koishibot.network.DataRegistry;
import io.github.nickid2018.koishibot.network.SerializableData;
import io.netty.buffer.Unpooled;

import java.util.UUID;

public class UserInfoQuery implements SerializableData {

    private final Environment env;

    public UUID queryId = UUID.randomUUID();
    public String id;
    public boolean isStranger;

    public UserInfoQuery(Environment env) {
        this.env = env;
    }

    @Override
    public void write(ByteData buf) {
        buf.writeUUID(queryId);
        buf.writeString(id);
        buf.writeBoolean(isStranger);
    }

    @Override
    public void read(ByteData buf) {
        queryId = buf.readUUID();
        id = buf.readString();
        isStranger = buf.readBoolean();
    }

    public static byte[] toBytes(UserInfo user) {
        ByteData buf = new ByteData(Unpooled.buffer());
        buf.writeSerializableData(user);
        byte[] data = buf.readByteArray();
        buf.release();
        return data;
    }

    public static UserInfo fromBytes(DataRegistry registry, byte[] data) {
        if (data == null)
            return null;
        ByteData buf = new ByteData(Unpooled.wrappedBuffer(data));
        UserInfo userInfo = buf.readSerializableData(registry, UserInfo.class);
        buf.release();
        return userInfo;
    }
}
