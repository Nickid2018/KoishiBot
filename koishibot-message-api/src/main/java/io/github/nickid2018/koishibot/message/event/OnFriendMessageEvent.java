package io.github.nickid2018.koishibot.message.event;

import io.github.nickid2018.koishibot.message.api.ChainMessage;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import io.github.nickid2018.koishibot.network.ByteData;
import io.github.nickid2018.koishibot.network.SerializableData;

public class OnFriendMessageEvent implements SerializableData {

    private final Environment env;
    public UserInfo user;
    public ChainMessage message;
    public long time;

    public OnFriendMessageEvent(Environment env) {
        this.env = env;
    }

    @Override
    public void read(ByteData buf) {
        user = buf.readSerializableData(env.getConnection().getRegistry(), UserInfo.class);
        message = buf.readSerializableData(env.getConnection().getRegistry(), ChainMessage.class);
        time = buf.readLong();
    }

    @Override
    public void write(ByteData buf) {
        buf.writeSerializableData(user);
        buf.writeSerializableData(message);
        buf.writeLong(time);
    }
}
