package io.github.nickid2018.koishibot.message.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public interface AudioMessage extends AbstractMessage {

    AudioMessage fillAudio(InputStream source) throws IOException;
}
