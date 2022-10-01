package io.github.nickid2018.koishibot.message.telegram;

import io.github.nickid2018.koishibot.message.api.ContactInfo;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import org.telegram.telegrambots.meta.api.objects.Chat;

public class TelegramGroup implements GroupInfo {

    private final Chat chat;
    private final TelegramEnvironment environment;

    public TelegramGroup(TelegramEnvironment environment, Chat chat) {
        this.environment = environment;
        this.chat = chat;
    }

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public boolean equals(ContactInfo info) {
        return info instanceof TelegramGroup && ((TelegramGroup) info).chat.getId().equals(chat.getId());
    }

    @Override
    public String getName() {
        return chat.getTitle();
    }

    @Override
    public String getGroupId() {
        return "tg.group" + chat.getId();
    }

    public Chat getChat() {
        return chat;
    }
}
