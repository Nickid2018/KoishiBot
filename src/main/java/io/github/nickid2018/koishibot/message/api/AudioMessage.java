package io.github.nickid2018.koishibot.message.api;

import java.io.IOException;
import java.io.InputStream;

public interface AudioMessage extends AbstractMessage {

    AudioMessage fillAudio(GroupInfo group, InputStream source) throws IOException;
}
