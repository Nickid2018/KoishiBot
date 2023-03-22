package io.github.nickid2018.koishibot.message.telegram;

import io.github.nickid2018.koishibot.message.api.ImageMessage;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;

import java.io.File;
import java.io.IOException;
import java.util.Random;

public class TelegramImage extends ImageMessage implements TelegramMessage {

    public static final Random RANDOM = new Random();
    private Message sentMessage;

    public TelegramImage(TelegramEnvironment environment) {
        super(environment);
    }

    public TelegramImage(TelegramEnvironment environment, PhotoSize photo) throws Exception {
        super(environment);
        GetFile getFile = new GetFile();
        getFile.setFileId(photo.getFileId());
        File dest = File.createTempFile("koishibot-tg", ".png");
        dest.deleteOnExit();
        imageSource = environment.getBot().downloadFile(photo.getFileId(), dest).toURI().toURL();
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
        InputFile file = new InputFile();
        try {
            file.setMedia(imageSource.openStream(), RANDOM.nextInt() + ".png");
            data.setImageSend(file);
        } catch (IOException e) {
            TelegramEnvironment.LOGGER.error("Failed to send image", e);
        }
    }
}
