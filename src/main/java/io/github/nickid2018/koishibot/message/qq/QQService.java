package io.github.nickid2018.koishibot.message.qq;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.github.nickid2018.koishibot.message.api.ServiceMessage;
import io.github.nickid2018.koishibot.util.Either;
import net.mamoe.mirai.message.data.LightApp;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.RichMessage;
import net.mamoe.mirai.message.data.SimpleServiceMessage;

public class QQService extends QQMessage implements ServiceMessage {

    private RichMessage richMessage;

    protected QQService(QQEnvironment environment) {
        super(environment);
    }

    protected QQService(QQEnvironment environment, RichMessage richMessage) {
        super(environment);
        this.richMessage = richMessage;
    }

    @Override
    public ServiceMessage fillService(String name, Either<JsonObject, String> data) {
        if (data.isLeft())
            richMessage = new LightApp(data.getLeft().toString());
        else
            richMessage = new SimpleServiceMessage(60, data.getRight());
        return this;
    }

    @Override
    public String getName() {
        Either<JsonObject, String> data = getData();
        if (data.isLeft())
            return data.getLeft().get("desc").getAsString();
        return "<Unknown>";
    }

    @Override
    public Either<JsonObject, String> getData() {
        try {
            return Either.left(JsonParser.parseString(richMessage.getContent()).getAsJsonObject());
        } catch (JsonSyntaxException e) {
            return Either.right(richMessage.getContent());
        }
    }

    @Override
    protected Message getQQMessage() {
        return richMessage;
    }
}
