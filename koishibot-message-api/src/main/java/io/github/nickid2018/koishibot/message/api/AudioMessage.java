package io.github.nickid2018.koishibot.message.api;

import io.github.nickid2018.koishibot.network.ByteData;

import java.net.MalformedURLException;
import java.net.URL;

public class AudioMessage extends AbstractMessage {

    protected GroupInfo group;
    protected URL audioSource;

    public AudioMessage(Environment env) {
        super(env);
    }

    public AudioMessage fillAudio(GroupInfo group, URL source) {
        this.group = group;
        this.audioSource = source;
        return this;
    }

    @Override
    protected void readAdditional(ByteData buf) {
        group = buf.readSerializableDataOrNull(env.getConnection(), GroupInfo.class);
        try {
            audioSource = new URL(buf.readString());
        } catch (MalformedURLException e) {
            audioSource = null;
        }
    }

    @Override
    protected void writeAdditional(ByteData buf) {
        buf.writeSerializableDataOrNull(env.getConnection().getRegistry(), group);
        buf.writeString(audioSource.toString());
    }

    public GroupInfo getGroup() {
        return group;
    }

    public URL getSourceURL() {
        return audioSource;
    }
}
