package io.github.nickid2018.koishibot.message;

import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.network.Connection;
import io.github.nickid2018.koishibot.util.AsyncUtil;
import io.github.nickid2018.koishibot.util.FormatTransformer;

import java.io.File;
import java.net.URL;
import java.util.concurrent.Future;

public class DelegateEnvironment extends Environment {

    private final MessageSender sender;
    private final MessageManager manager;

    public DelegateEnvironment(Connection connection) {
        super(connection);
        sender = new MessageSender(this, needAntiFilter);
        manager = new MessageManager(this);
    }

    public MessageSender getMessageSender() {
        return sender;
    }

    public MessageManager getMessageManager() {
        return manager;
    }

    public Future<File[]> parseAudioFile(String suffix, URL url) {
        if (audioSilk)
            return AsyncUtil.submit(() -> FormatTransformer.transformWebAudioToSilks(suffix, url));
        else
            return AsyncUtil.submit(() -> FormatTransformer.transformWebAudioToMP3(suffix, url));
    }
}
