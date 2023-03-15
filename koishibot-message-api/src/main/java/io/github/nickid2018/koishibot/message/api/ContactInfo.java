package io.github.nickid2018.koishibot.message.api;

import io.github.nickid2018.koishibot.network.SerializableData;

public abstract class ContactInfo implements SerializableData {

    protected final Environment env;

    public ContactInfo(Environment env) {
        this.env = env;
    }

    public Environment getEnvironment() {
        return env;
    }

    public abstract boolean equals(ContactInfo info);

    public abstract String getName();

    public void send(AbstractMessage message) {
        if (this instanceof GroupInfo)
            message.send((GroupInfo) this);
        else
            message.send((UserInfo) this);
    }
}
