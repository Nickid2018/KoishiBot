package io.github.nickid2018.koishibot.message.event;

import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import io.github.nickid2018.koishibot.network.ByteData;
import io.github.nickid2018.koishibot.network.SerializableData;

public class OnFriendRecallEvent implements SerializableData {

    private final Environment env;
    public UserInfo user;
    public long time;

    public OnFriendRecallEvent(Environment env) {
        this.env = env;
    }

    @Override
    public void read(ByteData buf) {
        user = buf.readSerializableData(env.getConnection(), UserInfo.class);
        time = buf.readLong();
    }

    @Override
    public void write(ByteData buf) {
        buf.writeSerializableData(user);
        buf.writeLong(time);
    }
}
