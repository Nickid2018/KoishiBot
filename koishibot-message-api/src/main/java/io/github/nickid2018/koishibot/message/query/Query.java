package io.github.nickid2018.koishibot.message.query;

import io.github.nickid2018.koishibot.network.ByteData;
import io.github.nickid2018.koishibot.network.SerializableData;

import java.util.UUID;

public abstract class Query implements SerializableData {

    public UUID queryId = UUID.randomUUID();

    @Override
    public void write(ByteData buf) {
        buf.writeUUID(queryId);
    }

    @Override
    public void read(ByteData buf) {
        queryId = buf.readUUID();
    }
}
