package io.github.nickid2018.koishibot.message.api;

import io.github.nickid2018.koishibot.message.query.NameInGroupQuery;
import io.github.nickid2018.koishibot.message.action.NudgeAction;
import io.github.nickid2018.koishibot.network.ByteData;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class UserInfo extends ContactInfo {
    protected boolean isStranger;
    protected String userId;
    protected String name;

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
        Future<byte[]> future = env.getListener().queryData(query.queryId);
        env.getConnection().sendPacket(query);
        try {
            byte[] data = future.get();
            if (data == null)
                return "<error>";
            return new String(data, StandardCharsets.UTF_8);
        } catch (InterruptedException | ExecutionException e) {
            return "<error>";
        }
    }

    @Override
    public boolean equals(ContactInfo info) {
        return info.getEnvironment() == env && info instanceof UserInfo user && user.getUserId().equals(userId);
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
