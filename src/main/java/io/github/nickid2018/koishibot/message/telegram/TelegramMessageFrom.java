package io.github.nickid2018.koishibot.message.telegram;

import io.github.nickid2018.koishibot.message.api.MessageFrom;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class TelegramMessageFrom implements MessageFrom {

    private final TelegramEnvironment environment;
    private final String chatID;
    private final int msgID;

    public TelegramMessageFrom(TelegramEnvironment environment, String chatID, int msgID) {
        this.environment = environment;
        this.chatID = chatID;
        this.msgID = msgID;
    }

    @Override
    public boolean equals(MessageFrom source) {
        return source instanceof TelegramMessageFrom from && from.msgID == msgID;
    }

    @Override
    public void recall() {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setMessageId(msgID);
        deleteMessage.setChatId(chatID);
        try {
            environment.getBot().execute(deleteMessage);
        } catch (TelegramApiException ignored) {
        }
    }
}
