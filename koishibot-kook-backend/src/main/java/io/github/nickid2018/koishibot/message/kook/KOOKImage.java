package io.github.nickid2018.koishibot.message.kook;

import io.github.kookybot.message.SelfMessage;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.message.api.ImageMessage;
import io.github.nickid2018.koishibot.network.ByteData;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class KOOKImage extends ImageMessage implements KOOKMessage {

    private io.github.kookybot.message.ImageMessage imageMessage;
    private SelfMessage sentMessage;

    public KOOKImage(KOOKEnvironment environment) {
        super(environment);
    }

    public KOOKImage(KOOKEnvironment environment, io.github.kookybot.message.ImageMessage image) {
        super(environment);
        this.imageMessage = image;
        try {
            this.imageSource = new URL(image.content());
        } catch (MalformedURLException e) {
            this.imageSource = null;
        }
    }

    @Override
    public void send(GroupInfo group) {
        sentMessage = ((KOOKEnvironment) env).getKookClient().sendChannelMessage(2,
                ((KOOKTextChannel) group).getChannel(), null, null, imageMessage.content(), null);
    }

    @Override
    public void read(ByteData buf) {
        super.read(buf);
        File file = new File(imageSource.getFile());
        imageMessage = new io.github.kookybot.message.ImageMessage(
                ((KOOKEnvironment) env).getKookClient(), null, null, file);
    }

    @Override
    public void formatMessage(KOOKMessageData data) {
    }

    @Override
    public void setSentMessage(SelfMessage message) {
        sentMessage = message;
        source = new KOOKMessageSource(env, message.getId(), message);
    }

    @Override
    public SelfMessage getSentMessage() {
        return sentMessage;
    }
}
