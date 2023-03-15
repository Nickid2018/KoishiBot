package io.github.nickid2018.koishibot.message.api;

import io.github.nickid2018.koishibot.network.ByteData;

import java.net.MalformedURLException;
import java.net.URL;

public class ImageMessage extends AbstractMessage {

    protected URL source;

    public ImageMessage(Environment env) {
        super(env);
    }

    public ImageMessage fillImage(URL source) {
        this.source = source;
        return this;
    }

    public URL getImage() {
        return source;
    }

    @Override
    protected void readAdditional(ByteData buf) {
        try {
            source = new URL(buf.readString());
        } catch (MalformedURLException e) {
            source = null;
        }
    }

    @Override
    protected void writeAdditional(ByteData buf) {
        buf.writeString(source.toString());
    }
}
