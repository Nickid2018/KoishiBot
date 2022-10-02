package io.github.nickid2018.koishibot.message.telegram;

import io.github.nickid2018.koishibot.message.api.AudioMessage;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.util.func.SupplierE;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.Audio;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public class TelegramAudio extends TelegramMessage implements AudioMessage {

    public static final Random RANDOM = new Random();

    private SupplierE<InputStream, IOException> source;

    public TelegramAudio(TelegramEnvironment environment) {
        super(environment);
    }

    public TelegramAudio(TelegramEnvironment environment, Audio audio) {
        super(environment);
        source = () -> {
            try {
                GetFile getFile = new GetFile();
                getFile.setFileId(audio.getFileId());
                return environment.getBot().downloadFileAsStream(environment.getBot().execute(getFile).getFilePath());
            } catch (Exception e) {
                throw new IOException(e);
            }
        };
    }

    @Override
    public void formatMessage(TelegramMessageData data) {
        InputFile file = new InputFile();
        try {
            file.setMedia(source.get(), RANDOM.nextInt() + ".mp3");
            data.setAudioSend(file);
        } catch (IOException e) {
            TelegramEnvironment.LOGGER.error("Failed to send audio", e);
        }
    }

    @Override
    public AudioMessage fillAudio(GroupInfo group, InputStream source) {
        this.source = () -> source;
        return this;
    }
}
