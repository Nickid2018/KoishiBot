package io.github.nickid2018.koishibot.message.qq;

import io.github.nickid2018.koishibot.message.api.ImageMessage;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.message.data.Image;
import net.mamoe.mirai.message.data.Message;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class QQImage extends QQMessage implements ImageMessage {

    protected Image image;

    protected QQImage(QQEnvironment environment) {
        super(environment);
    }

    protected QQImage(QQEnvironment environment, Image image) {
        super(environment);
        this.image = image;
    }

    @Override
    public ImageMessage fillImage(InputStream stream) {
        image = Contact.uploadImage(environment.getBot().getAsFriend(), stream);
        return this;
    }

    @Override
    public InputStream getImage() throws IOException {
        return new URL(Image.queryUrl(image)).openStream();
    }

    @Override
    protected Message getQQMessage() {
        return image;
    }
}
