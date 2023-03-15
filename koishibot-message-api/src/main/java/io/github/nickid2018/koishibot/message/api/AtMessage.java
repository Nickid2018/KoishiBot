package io.github.nickid2018.koishibot.message.api;

import io.github.nickid2018.koishibot.network.ByteData;

import javax.annotation.Nonnull;

public class AtMessage extends AbstractMessage {

    private GroupInfo group;
    private UserInfo user;

    public AtMessage(Environment env) {
        super(env);
    }

    public AtMessage fillAt(GroupInfo group, UserInfo contact) {
        this.group = group;
        this.user = contact;
        return this;
    }

    @Nonnull
    public UserInfo getUser(GroupInfo group) {
        return user;
    }

    @Nonnull
    public String getId() {
        return user.getUserId();
    }

    @Override
    public void readAdditional(ByteData buf) {
        group = buf.readSerializableData(env.getConnection().getRegistry(), GroupInfo.class);
        user = buf.readSerializableData(env.getConnection().getRegistry(), UserInfo.class);
    }

    @Override
    public void writeAdditional(ByteData buf) {
        buf.writeSerializableData(group);
        buf.writeSerializableData(user);
    }
}
