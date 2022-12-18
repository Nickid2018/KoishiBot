package io.github.nickid2018.koishibot.resolver;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.message.MessageResolver;
import io.github.nickid2018.koishibot.message.ResolverName;
import io.github.nickid2018.koishibot.message.Syntax;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.util.AsyncUtil;
import io.github.nickid2018.koishibot.util.web.WebUtil;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.util.List;
import java.util.stream.StreamSupport;

@ResolverName("nbnhhsh")
@Syntax(syntax = "~nbnhhsh [查询内容]", help = "查询“能不能好好说话”")
public class NbnhhshResolver extends MessageResolver {

    public NbnhhshResolver() {
        super("~nbnhhsh");
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, Environment environment) {
        if (key.isEmpty())
            return false;
        AsyncUtil.execute(() -> {
            HttpPost post = new HttpPost("https://lab.magiconch.com/api/nbnhhsh/guess");
            JsonObject object = new JsonObject();
            object.addProperty("text", key);
            StringEntity entity = new StringEntity(object.toString(), ContentType.APPLICATION_JSON);
            post.setEntity(entity);
            try {
                JsonArray array = WebUtil.fetchDataInJson(post).getAsJsonArray();
                if (array.isEmpty()) {
                    environment.getMessageSender().sendMessage(context, environment.newText("未找到结果"));
                    return;
                }
                JsonObject find = array.get(0).getAsJsonObject();
                if (!find.has("trans")) {
                    environment.getMessageSender().sendMessage(context, environment.newText("未找到结果"));
                    return;
                }
                JsonArray findKeys = find.getAsJsonArray("trans");
                List<String> keys = StreamSupport.stream(findKeys.spliterator(), false)
                        .map(JsonElement::getAsString)
                        .limit(10)
                        .toList();
                String result = "「" + key + "」可能指：" + String.join(" ", keys);
                if (findKeys.size() > 10)
                    result += " 等" + findKeys.size() + "个结果";
                environment.getMessageSender().sendMessage(context, environment.newText(result));
            } catch (IOException e) {
                environment.getMessageSender().onError(e, "nbnhhsh", context, true);
            }
        });
        return true;
    }
}
