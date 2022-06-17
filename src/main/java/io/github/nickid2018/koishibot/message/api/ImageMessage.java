package io.github.nickid2018.koishibot.message.api;

import java.io.IOException;
import java.io.InputStream;

public interface ImageMessage extends AbstractMessage {

    ImageMessage fillImage(InputStream stream) throws IOException;

    InputStream getImage() throws IOException;
}
