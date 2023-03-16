package io.github.nickid2018.koishibot.message.query;

import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import io.github.nickid2018.koishibot.network.ByteData;

public class NameInGroupQuery extends Query {

    private final Environment env;

    public UserInfo user;
    public GroupInfo group;

    public NameInGroupQuery(Environment env) {
        this.env = env;
    }


    @Override
    public void read(ByteData buf) {
        super.read(buf);
        user = buf.readSerializableData(env.getConnection(), UserInfo.class);
        group = buf.readSerializableData(env.getConnection(), GroupInfo.class);
    }

    @Override
    public void write(ByteData buf) {
        super.write(buf);
        buf.writeSerializableData(user);
        buf.writeSerializableData(group);
    }
}
