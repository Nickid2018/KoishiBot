package io.github.nickid2018.koishibot.message.telegram;

import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.MessageSource;
import io.github.nickid2018.koishibot.message.api.UserInfo;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public interface TelegramMessage {

    static void send(TelegramMessage message, GroupInfo group) {
        if (group instanceof TelegramGroup info) {
            sendForChat(message, info.getChat().getId());
        }
    }

    static void send(TelegramMessage message, UserInfo contact) {
        if (contact instanceof TelegramUser info) {
            sendForChat(message, info.getUser().getId());
        }
    }

    static void sendForChat(TelegramMessage telegramMessage, long chatID) {
        TelegramMessageData data = new TelegramMessageData();
        telegramMessage.formatMessage(data);
        if (data.getImageSend() != null) {
            SendPhoto photo = new SendPhoto();
            photo.setChatId(chatID);
            photo.setPhoto(data.getImageSend());
            photo.setCaptionEntities(data.getMentionUsers());
            photo.setCaption(String.join("", data.getTexts()));
            photo.setReplyToMessageId(data.getQuoteID());
            try {
                telegramMessage.setSentMessage(((TelegramEnvironment) telegramMessage.getEnvironment()).getBot().execute(photo));
            } catch (TelegramApiException e) {
                TelegramEnvironment.LOGGER.error("Failed to send photo", e);
            }
        } else if (data.getAudioSend() != null) {
            SendAudio audio = new SendAudio();
            audio.setChatId(chatID);
            audio.setAudio(data.getAudioSend());
            audio.setCaptionEntities(data.getMentionUsers());
            audio.setCaption(String.join("", data.getTexts()));
            audio.setReplyToMessageId(data.getQuoteID());
            try {
                telegramMessage.setSentMessage(((TelegramEnvironment) telegramMessage.getEnvironment()).getBot().execute(audio));
            } catch (TelegramApiException e) {
                TelegramEnvironment.LOGGER.error("Failed to send audio", e);
            }
        } else {
            SendMessage message = new SendMessage();
            message.setEntities(data.getMentionUsers());
            message.setText(String.join("", data.getTexts()));
            message.setChatId(chatID);
            message.setReplyToMessageId(data.getQuoteID());
            try {
                telegramMessage.setSentMessage(((TelegramEnvironment) telegramMessage.getEnvironment()).getBot().execute(message));
            } catch (TelegramApiException e) {
                TelegramEnvironment.LOGGER.error("Failed to send message", e);
            }
        }
        MessageSource source = telegramMessage.getSource();
        if (source == null)
            telegramMessage.setMessageSource(new TelegramMessageSource((TelegramEnvironment) telegramMessage.getEnvironment()));
    }

    Environment getEnvironment();
    void setSentMessage(Message message);
    Message getSentMessage();
    MessageSource getSource();
    void setMessageSource(MessageSource source);
    void formatMessage(TelegramMessageData data);
}
