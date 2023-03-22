package io.github.nickid2018.koishibot.message.telegram;

import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.network.ByteData;
import org.telegram.telegrambots.meta.api.objects.Chat;

public class TelegramGroup extends GroupInfo {

    private Chat chat;

    public TelegramGroup(TelegramEnvironment environment) {
        super(environment);
    }

    public TelegramGroup(TelegramEnvironment environment, Chat chat) {
        super(environment);
        this.chat = chat;
        this.groupId = "tg.group" + chat.getId();
        this.name = chat.getTitle();
    }

    @Override
    public void read(ByteData buf) {
        super.read(buf);
        chat = ((TelegramEnvironment) env).getChat(groupId);
    }

    public Chat getChat() {
        return chat;
    }
}
