package io.github.nickid2018.koishibot.message.telegram;

import io.github.nickid2018.koishibot.message.api.AudioMessage;
import org.telegram.telegrambots.meta.api.objects.Audio;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Random;

public class TelegramAudio extends AudioMessage implements TelegramMessage {

    public static final Random RANDOM = new Random();
    private Message sentMessage;

    public TelegramAudio(TelegramEnvironment environment) {
        super(environment);
    }

    public TelegramAudio(TelegramEnvironment environment, Audio audio) {
        super(environment);
        try {
            audioSource = new File(".").toURI().toURL(); // Unsupported download
        } catch (MalformedURLException ignored) {
        }
        group = new TelegramGroup(environment);
        group.name = "<Unknown>";
        group.groupId = "tg.group-1";
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
            file.setMedia(audioSource.openStream(), RANDOM.nextInt() + ".mp3");
            data.setAudioSend(file);
        } catch (IOException e) {
            TelegramEnvironment.LOGGER.error("Failed to send audio", e);
        }
    }
}
