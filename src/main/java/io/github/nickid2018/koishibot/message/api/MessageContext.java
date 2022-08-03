package io.github.nickid2018.koishibot.message.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public record MessageContext(@Nullable GroupInfo group,
                             UserInfo user,
                             @Nonnull ChainMessage message, long sentTime) {

    public ContactInfo getSendDest() {
        return group != null ? group : user;
    }
}
