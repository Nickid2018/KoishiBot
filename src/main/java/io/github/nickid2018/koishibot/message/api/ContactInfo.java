package io.github.nickid2018.koishibot.message.api;

public interface ContactInfo {

    default void send(AbstractMessage message) {
        if (this instanceof GroupInfo)
            message.send((GroupInfo) this);
        else
            message.send((UserInfo) this);
    }

    Environment getEnvironment();

    boolean equals(ContactInfo info);

    String getName();
}
