package io.github.nickid2018.koishibot.message.qq;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.github.nickid2018.koishibot.message.api.ServiceMessage;
import io.github.nickid2018.koishibot.network.ByteData;
import io.github.nickid2018.koishibot.util.Either;
import io.github.nickid2018.koishibot.util.JsonUtil;
import net.mamoe.mirai.message.data.LightApp;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.RichMessage;
import net.mamoe.mirai.message.data.SimpleServiceMessage;

public class QQService extends ServiceMessage implements QQMessage {

    private RichMessage richMessage;

    protected QQService(QQEnvironment environment) {
        super(environment);
    }

    protected QQService(QQEnvironment environment, RichMessage richMessage) {
        super(environment);
        this.richMessage = richMessage;
        try {
            this.data = Either.left(JsonParser.parseString(richMessage.getContent()).getAsJsonObject());
            name =  JsonUtil.getStringInPathOrNull(data.left(), "meta.news.tag");
            if (name == null)
                name = JsonUtil.getStringInPathOrNull(data.left(), "desc");
            name = name == null ? "<Unknown>" : name;
        } catch (JsonSyntaxException e) {
            this.data = Either.right(richMessage.getContent());
            name = "<Unknown>";
        }
    }

    @Override
    public Message getMessage() {
        return richMessage;
    }

    @Override
    public void read(ByteData buf) {
        super.read(buf);
        if (data.isLeft())
            richMessage = new LightApp(data.left().toString());
        else
            richMessage = new SimpleServiceMessage(60, data.right());
    }
}
