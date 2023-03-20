package io.github.nickid2018.koishibot.message.action;

import io.github.nickid2018.koishibot.network.ByteData;
import io.github.nickid2018.koishibot.network.SerializableData;

public class StopAction implements SerializableData {

    public static final StopAction INSTANCE = new StopAction();

    @Override
    public void read(ByteData buf) {
    }

    @Override
    public void write(ByteData buf) {
    }
}
