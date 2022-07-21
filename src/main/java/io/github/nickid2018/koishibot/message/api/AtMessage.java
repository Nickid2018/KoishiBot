package io.github.nickid2018.koishibot.message.api;

import javax.annotation.Nonnull;

public interface AtMessage extends AbstractMessage {

    AtMessage fillAt(GroupInfo group, UserInfo contact);

    @Nonnull
    UserInfo getUser(GroupInfo group);

    @Nonnull
    String getId();
}
