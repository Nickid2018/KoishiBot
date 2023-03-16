package io.github.nickid2018.koishibot.message;

import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.network.Connection;

import java.io.File;
import java.net.URL;
import java.util.concurrent.Future;

public class DelegateEnvironment extends Environment {

    private final MessageSender sender;

    public DelegateEnvironment(Connection connection) {
        super(connection);
        sender = new MessageSender(this, false);
    }

    public MessageSender getMessageSender() {
        return sender;
    }

    public Future<File[]> parseAudioFile(String mp3, URL url) {
        return null; /**/
    }
}
