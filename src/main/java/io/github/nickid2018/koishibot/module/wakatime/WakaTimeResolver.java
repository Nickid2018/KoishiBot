package io.github.nickid2018.koishibot.module.wakatime;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.message.MessageResolver;
import io.github.nickid2018.koishibot.message.ResolverName;
import io.github.nickid2018.koishibot.message.Syntax;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.util.JsonUtil;
import io.github.nickid2018.koishibot.util.web.WebUtil;
import org.apache.http.client.methods.HttpGet;

import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

@ResolverName("wakatime")
@Syntax(syntax = "~wakatime", help = "获取现在的编程工作统计")
@Syntax(syntax = "~wakatime revoke", help = "取消授权")
public class WakaTimeResolver extends MessageResolver {

    public WakaTimeResolver() {
        super("~wakatime");
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, Environment environment) {
        WakaTimeModule.INSTANCE.getAuthenticator().authenticate(context.user().getUserId(),
                str -> environment.getMessageSender().sendMessage(context, environment.newText("请点击链接授权：\n" + str)),
                accessToken -> {
                    try {
                        HttpGet get = new HttpGet("https://wakatime.com/api/v1/users/current/stats/last_7_days");
                        get.setHeader("Authorization", "Bearer " + accessToken);
                        JsonObject data = WebUtil.fetchDataInJson(get).getAsJsonObject().getAsJsonObject("data");

                        StringBuilder builder = new StringBuilder();

                        builder.append("近七天编程时长: ").append(JsonUtil.getStringOrNull(data, "human_readable_total")).append("\n");
                        builder.append("平均编程时间: ").append(JsonUtil.getStringOrNull(data, "human_readable_daily_average")).append("\n");

                        JsonArray array = JsonUtil.getData(data, "languages", JsonArray.class).orElse(new JsonArray());
                        StreamSupport.stream(array.spliterator(), false)
                                .filter(JsonElement::isJsonObject)
                                .limit(5)
                                .map(e -> (JsonObject) e)
                                .forEach(object -> {
                                    String name = JsonUtil.getStringOrNull(object, "name");
                                    String percent = "%.2f".formatted(object.get("percent").getAsFloat());
                                    builder.append("语言").append(name).append(": ").append(percent).append("%\n");
                                });

                        environment.getMessageSender().sendMessage(context, environment.newText(builder.toString().trim()));
                    } catch (Exception e) {
                        environment.getMessageSender().onError(e, "wakatime", context, false);
                    }
                }, List.of("read_stats"), Map.of());
        return true;
    }
}
