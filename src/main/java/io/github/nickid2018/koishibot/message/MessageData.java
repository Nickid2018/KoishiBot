package io.github.nickid2018.koishibot.message;

import io.github.nickid2018.koishibot.message.api.AbstractMessage;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.UserInfo;

public class MessageData {

    public final GroupInfo group;
    public final UserInfo user;
    public final AbstractMessage sent;

    public MessageData(GroupInfo group, UserInfo user, AbstractMessage sent) {
        this.group = group;
        this.user = user;
        this.sent = sent;
    }
}
