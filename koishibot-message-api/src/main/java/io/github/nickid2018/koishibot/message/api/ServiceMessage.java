package io.github.nickid2018.koishibot.message.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.nickid2018.koishibot.network.ByteData;
import io.github.nickid2018.koishibot.network.SerializableData;
import io.github.nickid2018.koishibot.util.Either;

public class ServiceMessage extends AbstractMessage implements SerializableData {

    protected String name;
    protected Either<JsonObject, String> data;

    public ServiceMessage(Environment env) {
        super(env);
    }

    public ServiceMessage fillService(String name, Either<JsonObject, String> data) {
        this.name = name;
        this.data = data;
        return this;
    }

    public String getName() {
        return name;
    }

    public Either<JsonObject, String> getData() {
        return data;
    }


    @Override
    public void readAdditional(ByteData buf) {
        name = buf.readString();
        data = buf.readBoolean() ?
                Either.left(JsonParser.parseString(buf.readString()).getAsJsonObject()) :
                Either.right(buf.readString());
    }

    @Override
    public void writeAdditional(ByteData buf) {
        buf.writeString(name);
        if (data.isLeft()) {
            buf.writeBoolean(true);
            buf.writeString(data.left().toString());
        } else {
            buf.writeBoolean(false);
            buf.writeString(data.right());
        }
    }
}
