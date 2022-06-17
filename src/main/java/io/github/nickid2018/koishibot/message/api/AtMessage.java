package io.github.nickid2018.koishibot.message.api;

public interface AtMessage extends AbstractMessage {

    AtMessage fill(UserInfo contact);

    UserInfo getUser(GroupInfo group);
}
