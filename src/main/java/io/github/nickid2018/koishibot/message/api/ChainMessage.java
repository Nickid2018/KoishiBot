package io.github.nickid2018.koishibot.message.api;

public interface ChainMessage extends AbstractMessage {

    ChainMessage fill(AbstractMessage... messages);

    AbstractMessage[] getMessages();
}
