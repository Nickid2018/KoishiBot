package io.github.nickid2018.koishibot.message.api;

public class MessageContext {

    private final GroupInfo group;
    private final UserInfo user;
    private final ChainMessage message;

    public MessageContext(GroupInfo group, UserInfo user, ChainMessage message) {
        this.group = group;
        this.user = user;
        this.message = message;
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
}
