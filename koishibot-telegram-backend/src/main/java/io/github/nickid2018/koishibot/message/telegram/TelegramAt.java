package io.github.nickid2018.koishibot.message.telegram;

import io.github.nickid2018.koishibot.message.api.AtMessage;
import io.github.nickid2018.koishibot.network.ByteData;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.User;

public class TelegramAt extends AtMessage implements TelegramMessage {

    private User userAt;
    private Message sentMessage;

    public TelegramAt(TelegramEnvironment environment) {
        super(environment);
    }

    public TelegramAt(TelegramEnvironment environment, User user) {
        super(environment);
        this.userAt = user;
    }

    @Override
    public void setSentMessage(Message message) {
        sentMessage = message;
        source = new TelegramMessageSource((TelegramEnvironment) env, message.getChatId(), message.getMessageId());
    }

    @Override
    public Message getSentMessage() {
        return sentMessage;
    }

    @Override
    public void formatMessage(TelegramMessageData data) {
        MessageEntity entity = new MessageEntity();
        entity.setType("text_mention");
        entity.setOffset(data.getTexts().stream().mapToInt(String::length).sum());
        entity.setLength(userAt.getUserName().length());
        data.getMentionUsers().add(entity);
        data.getTexts().add("@" + userAt.getUserName());
    }

    @Override
    public void read(ByteData buf) {
        super.read(buf);
        userAt = ((TelegramUser) user).getUser();
    }
}
