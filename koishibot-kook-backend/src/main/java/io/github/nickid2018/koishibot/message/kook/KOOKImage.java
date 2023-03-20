package io.github.nickid2018.koishibot.message.kook;

import io.github.nickid2018.koishibot.core.TempFileSystem;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.ImageMessage;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class KOOKImage extends ImageMessage implements KOOKMessage {

    private io.github.kookybot.message.ImageMessage imageMessage;
    private File sendTemp;

    public KOOKImage(KOOKEnvironment environment) {
        super(environment);
    }

    public KOOKImage(KOOKEnvironment environment, io.github.kookybot.message.ImageMessage image) {
        super(environment);
        this.imageMessage = image;
        try {
            this.source = new URL(image.content());
        } catch (MalformedURLException e) {
            this.source = null;
        }
    }

    @Override
    public void send(GroupInfo group) {
        sentMessage = environment.getKookClient().sendChannelMessage(2,
                ((KOOKTextChannel) group).getChannel(), null, null, imageMessage.content(), null);
        TempFileSystem.unlockFileAndDelete(sendTemp);
    }

    @Override
    public ImageMessage fillImage(InputStream stream) throws IOException {
        sendTemp = TempFileSystem.createTmpFile("kook", "png");
        FileOutputStream fos = new FileOutputStream(sendTemp);
        IOUtils.copy(stream, fos);
        fos.close();
        imageMessage = new io.github.kookybot.message.ImageMessage(environment.getKookClient(), null, null, sendTemp);
        return this;
    }

    @Override
    public URL getImage() throws IOException {
        return new URL(imageMessage.content()).openStream();
    }

    @Override
    public void formatMessage(KOOKMessageData data) {
    }
}
