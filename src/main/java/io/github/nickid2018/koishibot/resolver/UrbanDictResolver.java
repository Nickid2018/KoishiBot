package io.github.nickid2018.koishibot.resolver;

import com.google.gson.JsonArray;
import io.github.nickid2018.koishibot.KoishiBotMain;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.util.JsonUtil;
import io.github.nickid2018.koishibot.util.WebUtil;
import org.apache.http.client.methods.HttpGet;

import java.util.regex.Pattern;

public class UrbanDictResolver extends MessageResolver {

    public static final String URBAN_API = "https://api.urbandictionary.com/v0/define?term=";

    public UrbanDictResolver() {
        super("~urban");
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Pattern pattern, Environment environment) {
        String term = key.trim();
        if (term.isEmpty())
            return false;
        KoishiBotMain.INSTANCE.executor.execute(() -> {
            try {
                JsonArray object = WebUtil.fetchDataInJson(
                        new HttpGet(URBAN_API + WebUtil.encode(term))).getAsJsonArray();
                if (object.size() == 0)
                    environment.getMessageSender().sendMessage(context, environment.newChain(
                            environment.newQuote(context.getMessage()),
                            environment.newText("未查找到该词语")
                    ));
                else {
                    StringBuilder builder = new StringBuilder();
                    if (object.size() > 1)
                        builder.append("查找出").append(object.size()).append("个结果，仅显示第一条\n");
                    builder.append(JsonUtil.getStringOrNull(object.get(0).getAsJsonObject(), "definition"));
                    environment.getMessageSender().sendMessage(context, environment.newChain(
                            environment.newQuote(context.getMessage()),
                            environment.newText(builder.toString().trim())
                    ));
                }
            } catch (Exception e) {
                environment.getMessageSender().onError(e, "urban", context, true);
            }
        });
        return true;
    }
}
