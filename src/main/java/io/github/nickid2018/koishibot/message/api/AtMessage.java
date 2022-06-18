package io.github.nickid2018.koishibot.message.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface AtMessage extends AbstractMessage {

    AtMessage fill(UserInfo contact);

    @Nullable
    UserInfo getUser(GroupInfo group);

    @Nonnull
    String getId();
}
