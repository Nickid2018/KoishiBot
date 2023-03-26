package io.github.nickid2018.koishibot.message.qq;

import io.github.nickid2018.koishibot.message.api.ImageMessage;
import io.github.nickid2018.koishibot.network.ByteData;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.message.data.Image;
import net.mamoe.mirai.message.data.Message;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class QQImage extends ImageMessage implements QQMessage {

    protected Image image;

    public QQImage(QQEnvironment environment) {
        super(environment);
    }

    protected QQImage(QQEnvironment environment, Image image) {
        super(environment);
        this.image = image;
        try {
            imageSource = new URL(Image.queryUrl(image));
        } catch (MalformedURLException ignored) {
        }
    }

    @Override
    public Message getMessage() {
        return image;
    }

    @Override
    public void read(ByteData buf) {
        super.read(buf);
        try {
            image = Contact.uploadImage(((QQEnvironment) env).getBot().getAsFriend(), imageSource.openStream());
        } catch (IOException e) {
            image = null;
        }
    }
}
