package io.github.nickid2018.koishibot.message.qq;

import io.github.nickid2018.koishibot.message.api.AudioMessage;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import net.mamoe.mirai.message.data.Audio;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.utils.ExternalResource;

import java.io.IOException;
import java.io.InputStream;

public class QQAudio extends QQMessage implements AudioMessage {

    private Audio audio;

    protected QQAudio(QQEnvironment environment) {
        super(environment);
    }

    protected QQAudio(QQEnvironment environment, Audio audio) {
        super(environment);
        this.audio = audio;
    }

    @Override
    public AudioMessage fillAudio(GroupInfo info, InputStream source) throws IOException {
        try (ExternalResource resource = ExternalResource.create(source)) {
            audio = ((QQGroup) info).getGroup().uploadAudio(resource);
        }
        return this;
    }

    @Override
    protected Message getQQMessage() {
        return audio;
    }
}
