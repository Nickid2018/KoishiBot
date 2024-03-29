package io.github.nickid2018.koishibot.message.api;

import io.github.nickid2018.koishibot.message.action.NudgeAction;
import io.github.nickid2018.koishibot.message.query.NameInGroupQuery;
import io.github.nickid2018.koishibot.network.ByteData;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class UserInfo extends ContactInfo {
    public boolean isStranger;
    public String userId;
    public String name;

    public UserInfo(Environment environment) {
        super(environment);
    }

    public String getUserId() {
        return userId;
    }

    public boolean isStranger() {
        return isStranger;
    }

    public void nudge(ContactInfo contact) {
        NudgeAction event = new NudgeAction(env);
        event.user = this;
        event.contact = contact;
        env.getConnection().sendPacket(event);
    }

    public String getNameInGroup(GroupInfo group) {
        NameInGroupQuery query = new NameInGroupQuery(env);
        query.group = group;
        query.user = this;
        CompletableFuture<byte[]> future = env.getListener().queryData(env.getConnection(), query);
        try {
            byte[] data = future.get(20, TimeUnit.SECONDS);
            if (data == null)
                return "<error>";
            return new String(data, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "<error>";
        }
    }

    @Override
    public boolean equals(ContactInfo info) {
        return info.getEnvironment().equals(env) && info instanceof UserInfo user && user.getUserId().equals(userId);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void read(ByteData buf) {
        userId = buf.readString();
        name = buf.readString();
        isStranger = buf.readBoolean();
    }

    @Override
    public void write(ByteData buf) {
        buf.writeString(userId);
        buf.writeString(name);
        buf.writeBoolean(isStranger);
    }
}
