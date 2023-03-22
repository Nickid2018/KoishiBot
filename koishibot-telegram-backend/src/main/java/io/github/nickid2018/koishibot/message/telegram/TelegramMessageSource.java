package io.github.nickid2018.koishibot.message.telegram;

import io.github.nickid2018.koishibot.message.api.MessageSource;
import io.github.nickid2018.koishibot.util.TimeoutCache;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class TelegramMessageSource extends MessageSource {

    public static TimeoutCache<TelegramMessageSource> messageCache = new TimeoutCache<>();

    private long chatID;
    private int msgID;

    public TelegramMessageSource(TelegramEnvironment environment) {
        super(environment);
    }

    public TelegramMessageSource(TelegramEnvironment environment, long chatID, int msgID) {
        super(environment);
        this.chatID = chatID;
        this.msgID = msgID;
        this.messageUniqueID = "tg.msg-%d-%d".formatted(chatID, msgID);
        messageCache.put(messageUniqueID, this, 1000 * 60 * 5);
    }

    public static void recall(TelegramMessageSource source) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setMessageId(source.msgID);
        deleteMessage.setChatId(source.chatID);
        try {
            ((TelegramEnvironment) source.env).getBot().execute(deleteMessage);
        } catch (TelegramApiException e) {
            TelegramEnvironment.LOGGER.error("Failed to delete message", e);
        }
    }
}
