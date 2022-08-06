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
import io.github.nickid2018.koishibot.util.web.OAuth2Authenticator;
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
        key = key.trim();
        if (key.isEmpty()) {
            WakaTimeModule.INSTANCE.getAuthenticator().authenticate(context.user().getUserId(),
                    str -> environment.getMessageSender().sendMessage(context, environment.newText("请点击链接授权：\n" + str)),
                    accessToken -> {
                        try {
                            HttpGet get = new HttpGet("https://wakatime.com/api/v1/users/current/stats/last_7_days");
                            get.setHeader("Authorization", "Bearer " + accessToken);
                            JsonObject data = WebUtil.fetchDataInJson(get).getAsJsonObject().getAsJsonObject("data");

                            StringBuilder builder = new StringBuilder();

                            builder.append(JsonUtil.getStringOrNull(data, "username")).append("\n");

                            builder.append("近七天编程时长: ").append(JsonUtil.getStringOrNull(data, "human_readable_total")).append("\n");
                            builder.append("平均编程时间: ").append(JsonUtil.getStringOrNull(data, "human_readable_daily_average")).append("\n");

                            builder.append("编程语言占比:\n");
                            JsonArray array = JsonUtil.getData(data, "languages", JsonArray.class).orElse(new JsonArray());
                            StreamSupport.stream(array.spliterator(), false)
                                    .filter(JsonElement::isJsonObject)
                                    .limit(5)
                                    .map(e -> (JsonObject) e)
                                    .forEach(object -> {
                                        String name = JsonUtil.getStringOrNull(object, "name");
                                        String percent = "%.2f".formatted(object.get("percent").getAsFloat());
                                        builder.append(name).append(": ").append(percent).append("%\n");
                                    });

                            environment.getMessageSender().sendMessage(context, environment.newText(builder.toString().trim()));
                        } catch (Exception e) {
                            environment.getMessageSender().onError(e, "wakatime", context, false);
                        }
                    }, List.of("read_stats"), Map.of());
            return true;
        } else if (key.equalsIgnoreCase("revoke")) {
            OAuth2Authenticator authenticator = WakaTimeModule.INSTANCE.getAuthenticator();
            if (authenticator.authenticated(context.user().getUserId())) {
                authenticator.revoke(context.user().getUserId());
                environment.getMessageSender().sendMessage(context, environment.newText("已撤销对此bot的授权"));
            } else
                environment.getMessageSender().sendMessage(context, environment.newText("用户未授权"));
            return true;
        }
        return false;
    }
}
