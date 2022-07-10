package io.github.nickid2018.koishibot.message.kook;

import io.github.nickid2018.koishibot.core.TempFileSystem;
import io.github.nickid2018.koishibot.message.api.ImageMessage;
import io.github.nickid2018.koishibot.util.value.Either;
import io.github.zly2006.kookybot.message.Message;
import io.github.zly2006.kookybot.message.MessageComponent;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class KOOKImage extends KOOKMessage implements ImageMessage {

    private io.github.zly2006.kookybot.message.ImageMessage image;

    public KOOKImage(KOOKEnvironment environment) {
        super(environment);
    }

    public KOOKImage(KOOKEnvironment environment, io.github.zly2006.kookybot.message.ImageMessage image) {
        super(environment);
        this.image = image;
    }

    @Override
    public ImageMessage fillImage(InputStream stream) throws IOException {
        File tmp = TempFileSystem.createTmpFile("kook", "image");
        FileOutputStream fos = new FileOutputStream(tmp);
        IOUtils.copy(stream, fos);
        fos.close();
        image = new io.github.zly2006.kookybot.message.ImageMessage(environment.getKookClient(), null, null, tmp);
        return this;
    }

    @Override
    public InputStream getImage() throws IOException {
        return new URL(image.content()).openStream();
    }

    @Override
    public Either<Message, MessageComponent> getKOOKMessage() {
        return Either.left(image);
    }
}
