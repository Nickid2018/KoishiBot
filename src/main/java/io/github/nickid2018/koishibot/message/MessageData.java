package io.github.nickid2018.koishibot.message;

import io.github.nickid2018.koishibot.message.api.AbstractMessage;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.UserInfo;

public record MessageData(GroupInfo group,
                          UserInfo user,
                          AbstractMessage sent) {
}
