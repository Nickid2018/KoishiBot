package io.github.nickid2018.koishibot.message.qq;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.github.nickid2018.koishibot.message.api.ServiceMessage;
import io.github.nickid2018.koishibot.util.value.Either;
import io.github.nickid2018.koishibot.util.JsonUtil;
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
            richMessage = new LightApp(data.left().toString());
        else
            richMessage = new SimpleServiceMessage(60, data.right());
        return this;
    }

    @Override
    public String getName() {
        Either<JsonObject, String> data = getData();
        if (data.isLeft()) {
            String name =  JsonUtil.getStringInPathOrNull(data.left(), "meta.news.tag");
            if (name == null)
                name = JsonUtil.getStringInPathOrNull(data.left(), "desc");
            return name == null ? "<Unknown>" : name;
        }
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
    public Message getQQMessage() {
        return richMessage;
    }
}
