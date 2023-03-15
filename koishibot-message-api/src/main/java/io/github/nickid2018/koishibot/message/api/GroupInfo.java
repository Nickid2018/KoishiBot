package io.github.nickid2018.koishibot.message.api;

import io.github.nickid2018.koishibot.network.ByteData;

public class GroupInfo extends ContactInfo {

    private String groupId;
    private String name;

    public GroupInfo(Environment env) {
        super(env);
    }

    public String getGroupId() {
        return groupId;
    }

    @Override
    public boolean equals(ContactInfo info) {
        return env.equals(info.getEnvironment()) && info instanceof GroupInfo group && groupId.equals(group.groupId);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void read(ByteData buf) {
        groupId = buf.readString();
        name = buf.readString();
    }

    @Override
    public void write(ByteData buf) {
        buf.writeString(groupId);
        buf.writeString(name);
    }
}
