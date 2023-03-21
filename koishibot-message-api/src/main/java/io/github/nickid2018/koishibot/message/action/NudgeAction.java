package io.github.nickid2018.koishibot.message.action;

import io.github.nickid2018.koishibot.message.api.ContactInfo;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import io.github.nickid2018.koishibot.network.ByteData;
import io.github.nickid2018.koishibot.network.SerializableData;

public class NudgeAction implements SerializableData {

    private final Environment env;
    public UserInfo user;
    public ContactInfo contact;

    public NudgeAction(Environment env) {
        this.env = env;
    }

    @Override
    public void read(ByteData buf) {
        user = buf.readSerializableData(env.getConnection(), UserInfo.class);
        contact = (ContactInfo) buf.readSerializableData(env.getConnection());
    }

    @Override
    public void write(ByteData buf) {
        buf.writeSerializableData(user);
        buf.writeSerializableDataMultiChoice(env.getConnection().getRegistry(), contact);
    }
}
