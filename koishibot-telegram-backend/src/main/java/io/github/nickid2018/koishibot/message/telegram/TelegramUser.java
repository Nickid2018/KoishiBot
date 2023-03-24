package io.github.nickid2018.koishibot.message.telegram;

import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import io.github.nickid2018.koishibot.network.ByteData;
import io.github.nickid2018.koishibot.util.TimeoutCache;
import org.telegram.telegrambots.meta.api.objects.User;

public class TelegramUser extends UserInfo {

    public static final TimeoutCache<TelegramUser> USER_CACHE = new TimeoutCache<>();

    private User user;

    public TelegramUser(TelegramEnvironment environment) {
        super(environment);
    }

    public TelegramUser(TelegramEnvironment environment, User user) {
        super(environment);
        this.user = user;
        name = user.getUserName();
        userId = "tg.user" + user.getId();
        isStranger = false;
        USER_CACHE.put(userId, this, 10000000_000L);
    }

    public static String getNameInGroup(UserInfo userInfo, GroupInfo group) {
        return userInfo.getName();
    }

    public User getUser() {
        return user;
    }

    @Override
    public void read(ByteData buf) {
        super.read(buf);
        if (USER_CACHE.containsKey(userId)) {
            TelegramUser telegramUser = USER_CACHE.get(userId);
            user = telegramUser.user;
        }
    }
}
