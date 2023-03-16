package io.github.nickid2018.koishibot.message.query;

import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.network.ByteData;
import io.github.nickid2018.koishibot.network.DataRegistry;
import io.netty.buffer.Unpooled;

public class GroupInfoQuery extends Query {

    private final Environment env;
    public String id;

    public GroupInfoQuery(Environment env) {
        this.env = env;
    }

    @Override
    public void read(ByteData buf) {
        super.read(buf);
        id = buf.readString();
    }

    @Override
    public void write(ByteData buf) {
        super.write(buf);
        buf.writeString(id);
    }

    public static byte[] toBytes(GroupInfo groupInfo) {
        ByteData buf = new ByteData(Unpooled.buffer());
        buf.writeSerializableData(groupInfo);
        byte[] data = buf.readByteArray();
        buf.release();
        return data;
    }

    public static GroupInfo fromBytes(DataRegistry registry, byte[] data) {
        if (data == null)
            return null;
        ByteData buf = new ByteData(Unpooled.wrappedBuffer(data));
        GroupInfo userInfo = buf.readSerializableData(registry, GroupInfo.class);
        buf.release();
        return userInfo;
    }
}
