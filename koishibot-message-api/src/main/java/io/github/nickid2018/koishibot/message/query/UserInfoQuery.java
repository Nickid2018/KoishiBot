package io.github.nickid2018.koishibot.message.query;

import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import io.github.nickid2018.koishibot.network.ByteData;
import io.github.nickid2018.koishibot.network.Connection;
import io.netty.buffer.Unpooled;

public class UserInfoQuery extends Query {

    private final Environment env;

    public String id;
    public boolean isStranger;

    public UserInfoQuery(Environment env) {
        this.env = env;
    }

    @Override
    public void write(ByteData buf) {
        super.write(buf);
        buf.writeString(id);
        buf.writeBoolean(isStranger);
    }

    @Override
    public void read(ByteData buf) {
        super.read(buf);
        id = buf.readString();
        isStranger = buf.readBoolean();
    }

    public static byte[] toBytes(UserInfo user) {
        ByteData buf = new ByteData(Unpooled.buffer());
        buf.writeSerializableData(user);
        byte[] data = buf.toByteArray();
        buf.release();
        return data;
    }

    public static UserInfo fromBytes(Connection connection, byte[] data) {
        if (data == null)
            return null;
        ByteData buf = new ByteData(Unpooled.wrappedBuffer(data));
        UserInfo userInfo = buf.readSerializableData(connection, UserInfo.class);
        buf.release();
        return userInfo;
    }
}
