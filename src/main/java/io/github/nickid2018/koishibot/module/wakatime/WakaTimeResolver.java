package io.github.nickid2018.koishibot.module.wakatime;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.nickid2018.koishibot.message.MessageResolver;
import io.github.nickid2018.koishibot.message.ResolverName;
import io.github.nickid2018.koishibot.message.Syntax;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.util.JsonUtil;
import io.github.nickid2018.koishibot.util.RegexUtil;
import io.github.nickid2018.koishibot.util.web.OAuth2Authenticator;
import io.github.nickid2018.koishibot.util.web.WebUtil;
import org.apache.http.client.methods.HttpGet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

@ResolverName("wakatime")
@Syntax(syntax = "~wakatime", help = "获取最近7天的编程工作统计", rem = "此数据每天凌晨0点更新")
@Syntax(syntax = "~wakatime today", help = "获取今天的编程工作统计", rem = "此数据随时更新")
@Syntax(syntax = "~wakatime yesterday", help = "获取昨天的编程工作统计")
@Syntax(syntax = "~wakatime [时间区段]", help = "获取一定时间内的编程工作统计", rem = "时间格式为yyyymmdd-yyyymmdd")
@Syntax(syntax = "~wakatime revoke", help = "取消授权")
public class WakaTimeResolver extends MessageResolver {

    public static final Pattern TIME_DURATION = Pattern.compile("\\d{8}-\\d{8}");

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
                                    .map(JsonElement::getAsJsonObject)
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
        } else if (key.equalsIgnoreCase("today")) {
            formatSummary("https://wakatime.com/api/v1/users/current/summaries?range=Today", context, environment);
            return true;
        } else if (key.equalsIgnoreCase("yesterday")) {
            formatSummary("https://wakatime.com/api/v1/users/current/summaries?range=Yesterday", context, environment);
            return true;
        } else {
            if (!RegexUtil.match(TIME_DURATION, key))
                return false;
            String[] time = key.split("-");
            formatSummary("https://wakatime.com/api/v1/users/current/summaries?start=%s&end=%s".formatted(time[0], time[1]), context, environment);
            return true;
        }
    }

    private static void formatSummary(String url, MessageContext context, Environment environment) {
        WakaTimeModule.INSTANCE.getAuthenticator().authenticate(context.user().getUserId(),
                str -> environment.getMessageSender().sendMessage(context, environment.newText("请点击链接授权：\n" + str)),
                accessToken -> {
                    try {
                        HttpGet get = new HttpGet(url);
                        get.setHeader("Authorization", "Bearer " + accessToken);

                        JsonObject gotta = WebUtil.fetchDataInJson(get).getAsJsonObject();
                        JsonObject cummulative_total = gotta.getAsJsonObject("cummulative_total");
                        double sec = JsonUtil.getData(cummulative_total, "seconds", JsonPrimitive.class)
                                .filter(JsonPrimitive::isNumber)
                                .map(JsonPrimitive::getAsDouble)
                                .orElse(0.0);

                        StringBuilder builder = new StringBuilder();

                        builder.append("编程时长: ").append(JsonUtil.getStringOrNull(cummulative_total, "text")).append("\n");

                        Map<String, Double> langTime = new HashMap<>();

                        JsonArray array = JsonUtil.getData(gotta, "data", JsonArray.class).orElse(new JsonArray());
                        StreamSupport.stream(array.spliterator(), false)
                                .filter(JsonElement::isJsonObject)
                                .map(JsonElement::getAsJsonObject)
                                .forEach(object -> {
                                    JsonArray languages = JsonUtil.getData(object, "languages", JsonArray.class).orElse(new JsonArray());
                                    StreamSupport.stream(languages.spliterator(), false)
                                            .filter(JsonElement::isJsonObject)
                                            .map(JsonElement::getAsJsonObject)
                                            .forEach(language -> {
                                                String lang = JsonUtil.getStringOrNull(language, "name");
                                                double data = langTime.computeIfAbsent(lang, n -> 0.0);
                                                langTime.put(lang, data +
                                                        JsonUtil.getData(language, "seconds", JsonPrimitive.class)
                                                                .filter(JsonPrimitive::isNumber)
                                                                .map(JsonPrimitive::getAsDouble)
                                                                .orElse(0.0));
                                            });
                                });

                        builder.append("编程语言占比:\n");

                        langTime.entrySet().stream()
                                .sorted((e1, e2) -> (int) (e2.getValue() - e1.getValue()))
                                .limit(5)
                                .forEach(dat -> {
                                    String name = dat.getKey();
                                    String percent = "%.2f".formatted(dat.getValue() / sec);
                                    builder.append(name).append(": ").append(percent).append("%\n");
                                });

                        environment.getMessageSender().sendMessage(context, environment.newText(builder.toString().trim()));
                    } catch (Exception e) {
                        environment.getMessageSender().onError(e, "wakatime", context, false);
                    }
                }, List.of("read_logged_time"), Map.of());
    }
}
