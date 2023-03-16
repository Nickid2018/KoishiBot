package io.github.nickid2018.koishibot.message.event;

import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import io.github.nickid2018.koishibot.network.ByteData;
import io.github.nickid2018.koishibot.network.SerializableData;

public class OnMemberAddEvent implements SerializableData {

    private final Environment env;
    public GroupInfo group;
    public UserInfo user;

    public OnMemberAddEvent(Environment env) {
        this.env = env;
    }

    @Override
    public void read(ByteData buf) {
        group = buf.readSerializableData(env.getConnection(), GroupInfo.class);
        user = buf.readSerializableData(env.getConnection(), UserInfo.class);
    }

    @Override
    public void write(ByteData buf) {
        buf.writeSerializableData(group);
        buf.writeSerializableData(user);
    }
}
