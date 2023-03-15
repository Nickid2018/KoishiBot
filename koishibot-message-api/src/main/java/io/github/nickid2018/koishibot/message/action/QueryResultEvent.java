package io.github.nickid2018.koishibot.message.action;

import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.network.ByteData;
import io.github.nickid2018.koishibot.network.SerializableData;

import java.util.UUID;

public class QueryResultEvent implements SerializableData {

    private final Environment env;

    public UUID queryId;
    public byte[] payload;

    public QueryResultEvent(Environment env) {
        this.env = env;
    }

    @Override
    public void read(ByteData buf) {
        queryId = buf.readUUID();
        payload = buf.readByteArray();
    }

    @Override
    public void write(ByteData buf) {
        buf.writeUUID(queryId);
        buf.writeByteArray(payload);
    }
}
