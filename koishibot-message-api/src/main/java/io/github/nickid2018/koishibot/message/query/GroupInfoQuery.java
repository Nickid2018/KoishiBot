package io.github.nickid2018.koishibot.message.query;

import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.network.ByteData;
import io.github.nickid2018.koishibot.network.DataRegistry;
import io.github.nickid2018.koishibot.network.SerializableData;
import io.netty.buffer.Unpooled;

import java.util.UUID;

public class GroupInfoQuery implements SerializableData {

    private final Environment env;
    public String id;
    public UUID queryId = UUID.randomUUID();

    public GroupInfoQuery(Environment env) {
        this.env = env;
    }

    @Override
    public void read(ByteData buf) {
        queryId = buf.readUUID();
        id = buf.readString();
    }

    @Override
    public void write(ByteData buf) {
        buf.writeUUID(queryId);
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
