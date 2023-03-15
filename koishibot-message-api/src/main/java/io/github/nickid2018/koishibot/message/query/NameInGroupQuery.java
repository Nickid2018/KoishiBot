package io.github.nickid2018.koishibot.message.query;

import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import io.github.nickid2018.koishibot.network.ByteData;
import io.github.nickid2018.koishibot.network.SerializableData;

import java.util.UUID;

public class NameInGroupQuery implements SerializableData {

    private final Environment env;

    public UserInfo user;
    public GroupInfo group;

    public UUID queryId = UUID.randomUUID();

    public NameInGroupQuery(Environment env) {
        this.env = env;
    }


    @Override
    public void read(ByteData buf) {
        queryId = buf.readUUID();
        user = buf.readSerializableData(env.getConnection().getRegistry(), UserInfo.class);
        group = buf.readSerializableData(env.getConnection().getRegistry(), GroupInfo.class);
    }

    @Override
    public void write(ByteData buf) {
        buf.writeUUID(queryId);
        buf.writeSerializableData(user);
        buf.writeSerializableData(group);
    }
}
