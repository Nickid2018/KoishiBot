package io.github.nickid2018.koishibot.message.action;

import io.github.nickid2018.koishibot.message.api.ContactInfo;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import io.github.nickid2018.koishibot.network.ByteData;
import io.github.nickid2018.koishibot.network.DataRegistry;
import io.github.nickid2018.koishibot.network.SerializableData;

public class NudgeEvent implements SerializableData {

    private final Environment env;
    public UserInfo user;
    public ContactInfo contact;

    public NudgeEvent(Environment env) {
        this.env = env;
    }

    @Override
    public void read(ByteData buf) {
        DataRegistry registry = env.getConnection().getRegistry();
        user = buf.readSerializableData(registry, UserInfo.class);
        contact = buf.readSerializableData(registry, ContactInfo.class);
    }

    @Override
    public void write(ByteData buf) {
        buf.writeSerializableData(user);
        buf.writeSerializableData(contact);
    }
}
