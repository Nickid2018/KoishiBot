package io.github.nickid2018.koishibot.message.api;

import io.github.nickid2018.koishibot.network.ByteData;

import java.net.MalformedURLException;
import java.net.URL;

public class AudioMessage extends AbstractMessage {

    private GroupInfo group;
    private URL source;

    public AudioMessage(Environment env) {
        super(env);
    }

    public AudioMessage fillAudio(GroupInfo group, URL source) {
        this.group = group;
        this.source = source;
        return this;
    }

    @Override
    protected void readAdditional(ByteData buf) {
        group = buf.readSerializableData(env.getConnection().getRegistry(), GroupInfo.class);
        try {
            source = new URL(buf.readString());
        } catch (MalformedURLException e) {
            source = null;
        }
    }

    @Override
    protected void writeAdditional(ByteData buf) {
        buf.writeSerializableData(group);
        buf.writeString(source.toString());
    }

    public GroupInfo getGroup() {
        return group;
    }

    public URL getSourceURL() {
        return source;
    }
}
