package io.github.nickid2018.koishibot.message.qq;

import io.github.nickid2018.koishibot.message.api.AudioMessage;
import io.github.nickid2018.koishibot.network.ByteData;
import net.mamoe.mirai.message.data.Audio;
import net.mamoe.mirai.utils.ExternalResource;

import java.io.IOException;
import java.net.URL;

public class QQAudio extends AudioMessage implements QQMessage {

    private Audio audio;

    protected QQAudio(QQEnvironment environment) {
        super(environment);
    }

    protected QQAudio(QQEnvironment environment, Audio audio) {
        super(environment);
        this.audio = audio;
        try {
            this.audioSource = new URL("https://github.com"); // Not supported now
            this.group = null;
        } catch (Exception ignored) {
        }
    }

    public Audio getMessage() {
        return audio;
    }

    @Override
    public void read(ByteData buf) {
        super.read(buf);
        try (ExternalResource resource = ExternalResource.create(audioSource.openStream())) {
            audio = ((QQGroup) group).getGroup().uploadAudio(resource);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
