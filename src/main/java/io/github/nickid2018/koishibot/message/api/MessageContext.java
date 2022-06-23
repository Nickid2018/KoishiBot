package io.github.nickid2018.koishibot.message.api;

public class MessageContext {

    private final GroupInfo group;
    private final UserInfo user;
    private final ChainMessage message;
    private final long sentTime;

    public MessageContext(GroupInfo group, UserInfo user, ChainMessage message, long sentTime) {
        this.group = group;
        this.user = user;
        this.message = message;
        this.sentTime = sentTime;
    }

    public ContactInfo getSendDest() {
        return group != null ? group : user;
    }

    public GroupInfo getGroup() {
        return group;
    }

    public UserInfo getUser() {
        return user;
    }

    public ChainMessage getMessage() {
        return message;
    }

    public long getSentTime() {
        return sentTime;
    }
}
