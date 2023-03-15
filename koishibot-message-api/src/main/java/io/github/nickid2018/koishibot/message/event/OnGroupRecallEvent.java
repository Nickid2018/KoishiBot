package io.github.nickid2018.koishibot.message.event;

import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import io.github.nickid2018.koishibot.network.ByteData;
import io.github.nickid2018.koishibot.network.SerializableData;

public class OnGroupRecallEvent implements SerializableData {

    private final Environment env;
    public GroupInfo group;
    public UserInfo user;
    public long time;

    public OnGroupRecallEvent(Environment env) {
        this.env = env;
    }

    @Override
    public void read(ByteData buf) {
        group = buf.readSerializableData(env.getConnection().getRegistry(), GroupInfo.class);
        user = buf.readSerializableData(env.getConnection().getRegistry(), UserInfo.class);
        time = buf.readLong();
    }

    @Override
    public void write(ByteData buf) {
        buf.writeSerializableData(group);
        buf.writeSerializableData(user);
        buf.writeLong(time);
    }
}
