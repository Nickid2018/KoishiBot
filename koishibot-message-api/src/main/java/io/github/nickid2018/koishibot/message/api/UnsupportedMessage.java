package io.github.nickid2018.koishibot.message.api;

import io.github.nickid2018.koishibot.network.ByteData;
import io.github.nickid2018.koishibot.network.SerializableData;

public class UnsupportedMessage extends AbstractMessage implements SerializableData {

    public UnsupportedMessage(Environment env) {
        super(env);
    }

    @Override
    public void send(UserInfo contact) {
    }

    @Override
    public void send(GroupInfo group) {
    }

    @Override
    public void recall() {
    }

    @Override
    public long getSentTime() {
        return -1;
    }

    @Override
    public MessageSource getSource() {
        return null;
    }

    @Override
    public void read(ByteData buf) {
    }

    @Override
    public void write(ByteData buf) {
    }
}
