package io.github.nickid2018.koishibot.message.api;

public interface AbstractMessage {

    Environment getEnvironment();

    void send(UserInfo contact);

    void send(GroupInfo group);

    void recall();

    long getSentTime();
}
