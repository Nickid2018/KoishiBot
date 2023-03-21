package io.github.nickid2018.koishibot.message.api;

import io.github.nickid2018.koishibot.network.ByteData;

import java.net.MalformedURLException;
import java.net.URL;

public class ImageMessage extends AbstractMessage {

    protected URL imageSource;

    public ImageMessage(Environment env) {
        super(env);
    }

    public ImageMessage fillImage(URL source) {
        this.imageSource = source;
        return this;
    }

    public URL getImage() {
        return imageSource;
    }

    @Override
    protected void readAdditional(ByteData buf) {
        try {
            imageSource = new URL(buf.readString());
        } catch (MalformedURLException e) {
            imageSource = null;
        }
    }

    @Override
    protected void writeAdditional(ByteData buf) {
        buf.writeString(imageSource.toString());
    }
}
