package io.github.nickid2018.koishibot.message.telegram;

import io.github.nickid2018.koishibot.message.api.AtMessage;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import org.jetbrains.annotations.NotNull;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.User;

public class TelegramAt extends TelegramMessage implements AtMessage {

    private User user;

    public TelegramAt(TelegramEnvironment environment) {
        super(environment);
    }

    public TelegramAt(TelegramEnvironment environment, User user) {
        super(environment);
        this.user = user;
    }

    @Override
    public void formatMessage(TelegramMessageData data) {
        MessageEntity entity = new MessageEntity();
        entity.setType("text_mention");
        entity.setOffset(data.getTexts().stream().mapToInt(String::length).sum());
        entity.setLength(user.getUserName().length());
        data.getMentionUsers().add(entity);
        data.getTexts().add("@" + user.getUserName());
    }

    @Override
    public AtMessage fillAt(GroupInfo group, UserInfo contact) {
        if (contact instanceof TelegramUser u) {
            user = u.getUser();
        }
        return this;
    }

    @NotNull
    @Override
    public UserInfo getUser(GroupInfo group) {
        return new TelegramUser(environment, user);
    }

    @NotNull
    @Override
    public String getId() {
        return "tg.user" + user.getId();
    }
}
