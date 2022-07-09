package io.github.nickid2018.koishibot.message.api;

public record MessageContext(GroupInfo group,
                             UserInfo user,
                             ChainMessage message, long sentTime) {

    public ContactInfo getSendDest() {
        return group != null ? group : user;
    }
}
