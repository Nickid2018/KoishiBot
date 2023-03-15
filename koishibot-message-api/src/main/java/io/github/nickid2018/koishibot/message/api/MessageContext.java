package io.github.nickid2018.koishibot.message.api;

import io.github.nickid2018.koishibot.network.ByteData;
import io.github.nickid2018.koishibot.network.DataRegistry;
import io.github.nickid2018.koishibot.network.SerializableData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MessageContext implements SerializableData {

    @Nonnull
    private final Environment environment;
    @Nullable
    private GroupInfo group;
    private UserInfo user;
    private ChainMessage message;
    private long sentTime;

    public MessageContext(@Nonnull Environment environment) {
        this.environment = environment;
    }

    public MessageContext(@Nonnull Environment environment,
                          @Nullable GroupInfo group,
                          UserInfo user,
                          @Nonnull ChainMessage message,
                          long sentTime) {
        this.environment = environment;
        this.group = group;
        this.user = user;
        this.message = message;
        this.sentTime = sentTime;
    }

    @Nullable
    public GroupInfo group() {
        return group;
    }

    public UserInfo user() {
        return user;
    }

    @Nonnull
    public ChainMessage message() {
        return message;
    }

    public long sentTime() {
        return sentTime;
    }

    public ContactInfo getSendDest() {
        return group != null ? group : user;
    }

    @Override
    public void read(ByteData buf) {
        DataRegistry registry = environment.getConnection().getRegistry();
        group = buf.readSerializableDataOrNull(registry, GroupInfo.class);
        user = buf.readSerializableData(registry, UserInfo.class);
        message = buf.readSerializableData(registry, ChainMessage.class);
        sentTime = buf.readLong();
    }

    @Override
    public void write(ByteData buf) {
        DataRegistry registry = environment.getConnection().getRegistry();
        buf.writeSerializableDataOrNull(registry, group);
        buf.writeSerializableData(user);
        buf.writeSerializableData(message);
        buf.writeLong(sentTime);
    }
}
