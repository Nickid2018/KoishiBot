package io.github.nickid2018.koishibot.message.kook;

import io.github.nickid2018.koishibot.core.TempFileSystem;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.ImageMessage;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class KOOKImage extends KOOKMessage implements ImageMessage {

    private io.github.kookybot.message.ImageMessage image;
    private File sendTemp;

    public KOOKImage(KOOKEnvironment environment) {
        super(environment);
    }

    public KOOKImage(KOOKEnvironment environment, io.github.kookybot.message.ImageMessage image) {
        super(environment);
        this.image = image;
    }

    @Override
    public void send(GroupInfo group) {
        sentMessage = environment.getKookClient().sendChannelMessage(2,
                ((KOOKTextChannel) group).getChannel(), null, null, image.content(), null);
        TempFileSystem.unlockFileAndDelete(sendTemp);
    }

    @Override
    public ImageMessage fillImage(InputStream stream) throws IOException {
        sendTemp = TempFileSystem.createTmpFile("kook", "png");
        FileOutputStream fos = new FileOutputStream(sendTemp);
        IOUtils.copy(stream, fos);
        fos.close();
        image = new io.github.kookybot.message.ImageMessage(environment.getKookClient(), null, null, sendTemp);
        return this;
    }

    @Override
    public InputStream getImage() throws IOException {
        return new URL(image.content()).openStream();
    }

    @Override
    public void formatMessage(KOOKMessageData data) {
    }
}
