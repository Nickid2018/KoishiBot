package io.github.nickid2018.koishibot.message.telegram;

import io.github.nickid2018.koishibot.message.api.ImageMessage;
import io.github.nickid2018.koishibot.util.func.SupplierE;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public class TelegramImage extends TelegramMessage implements ImageMessage {

    public static final Random RANDOM = new Random();

    private SupplierE<InputStream, IOException> imageStream;

    public TelegramImage(TelegramEnvironment environment) {
        super(environment);
    }

    public TelegramImage(TelegramEnvironment environment, PhotoSize photo) {
        super(environment);
        imageStream = () -> {
            try {
                GetFile getFile = new GetFile();
                getFile.setFileId(photo.getFileId());
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
            file.setMedia(imageStream.get(), RANDOM.nextInt() + ".png");
            data.setImageSend(file);
        } catch (IOException e) {
            TelegramEnvironment.LOGGER.error("Failed to send image", e);
        }
    }

    @Override
    public ImageMessage fillImage(InputStream stream) {
        imageStream = () -> stream;
        return this;
    }

    @Override
    public InputStream getImage() throws IOException {
        return imageStream.get();
    }
}
