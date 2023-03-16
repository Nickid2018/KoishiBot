package io.github.nickid2018.koishibot.message.api;

import io.github.nickid2018.koishibot.network.ByteData;

import javax.annotation.Nonnull;

public class AtMessage extends AbstractMessage {

    protected UserInfo user;

    public AtMessage(Environment env) {
        super(env);
    }

    public AtMessage fillAt(UserInfo contact) {
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
        user = buf.readSerializableData(env.getConnection(), UserInfo.class);
    }

    @Override
    public void writeAdditional(ByteData buf) {
        buf.writeSerializableData(user);
    }
}
