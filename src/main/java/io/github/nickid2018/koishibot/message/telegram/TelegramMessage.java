package io.github.nickid2018.koishibot.message.telegram;

import io.github.nickid2018.koishibot.message.api.*;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public abstract class TelegramMessage implements AbstractMessage {

    protected final TelegramEnvironment environment;
    protected Message sentMessage;

    public TelegramMessage(TelegramEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public void send(GroupInfo group) {
        if (group instanceof TelegramGroup info) {
            sendForChat(info.getChat().getId());
        }
    }

    @Override
    public void send(UserInfo contact) {
        if (contact instanceof TelegramUser info) {
            sendForChat(info.getUser().getId());
        }
    }

    private void sendForChat(long chatID) {
        TelegramMessageData data = new TelegramMessageData();
        formatMessage(data);
        if (data.getImageSend() != null) {
            SendPhoto photo = new SendPhoto();
            photo.setChatId(chatID);
            photo.setPhoto(data.getImageSend());
            photo.setCaptionEntities(data.getMentionUsers());
            photo.setCaption(String.join("", data.getTexts()));
            photo.setReplyToMessageId(data.getQuoteID());
            try {
                sentMessage = environment.getBot().execute(photo);
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
                sentMessage = environment.getBot().execute(audio);
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
                sentMessage = environment.getBot().execute(message);
            } catch (TelegramApiException e) {
                TelegramEnvironment.LOGGER.error("Failed to send message", e);
            }
        }
    }

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public void recall() {
        if (sentMessage != null) {
            DeleteMessage delete = new DeleteMessage();
            delete.setChatId(sentMessage.getChatId());
            delete.setMessageId(sentMessage.getMessageId());
            try {
                environment.getBot().execute(delete);
            } catch (TelegramApiException e) {
                TelegramEnvironment.LOGGER.error("Failed to delete message", e);
            }
        }
    }

    @Override
    public long getSentTime() {
        return sentMessage != null ? sentMessage.getDate() : -1;
    }

    @Override
    public MessageFrom getSource() {
        if (sentMessage != null)
            return new TelegramMessageFrom(environment, sentMessage.getChatId(), sentMessage.getMessageId());
        return null;
    }

    protected abstract void formatMessage(TelegramMessageData data);
}
