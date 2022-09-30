package io.github.nickid2018.koishibot.resolver;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.message.MessageResolver;
import io.github.nickid2018.koishibot.message.ResolverName;
import io.github.nickid2018.koishibot.message.Syntax;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.util.AsyncUtil;
import io.github.nickid2018.koishibot.util.JsonUtil;
import io.github.nickid2018.koishibot.util.RegexUtil;
import io.github.nickid2018.koishibot.util.web.WebUtil;
import org.apache.hc.client5.http.classic.methods.HttpGet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/*
 * API Documentation: https://docs.modrinth.com/api-spec/
 */
@ResolverName("mod-modrinth")
@Syntax(syntax = "<mod:[模组名称]>", help = "查询Modrinth模组信息")
@Syntax(syntax = "<mod:files:[模组名称]>", help = "查询Modrinth模组每个版本的最新文件")
@Syntax(syntax = "<mod:search:[查询模组的信息]>", help = "查询Modrinth模组信息")
@Syntax(syntax = "<mod:search:[页数],(候选数):[查询模组的信息]>", help = "指定页数和候选数查询Modrinth模组信息")
@Syntax(syntax = "<mod:search:[页数],(候选数):[查询模组的信息]|[查询指令]>", help = "指定查询指令、页数和候选数查询Modrinth模组信息")
public class ModrinthResolver extends MessageResolver {

    public static final Pattern MOD_PATTERN = Pattern.compile("<mod:.+?>");
    public static final Pattern MOD_SEARCH_PATTERN = Pattern.compile("<mod:search:.+?>");
    public static final Pattern MOD_FILES_PATTERN = Pattern.compile("<mod:files:.+?>");
    public static final Pattern MINECRAFT_VERSION = Pattern.compile("1\\.\\d{1,2}(\\.\\d)?");
    public static final Pattern SEARCH_PAGE_PATTERN = Pattern.compile("\\d*(,\\d+)?:.+");

    public static final String MODRINTH_API_URL = "https://api.modrinth.com/v2";

    public ModrinthResolver() {
        super(MOD_SEARCH_PATTERN, MOD_FILES_PATTERN, MOD_PATTERN);
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, Environment environment) {
        AsyncUtil.execute(() -> {
            try {
                if (resolvedArguments == MOD_SEARCH_PATTERN)
                    displaySearch(key.substring(12, key.length() - 1), context, environment);
                else if (resolvedArguments == MOD_FILES_PATTERN)
                    displayFiles(key.substring(11, key.length() - 1), context, environment);
                else
                    displayMod(key.substring(5, key.length() - 1), context, environment);
            } catch (Exception e) {
                environment.getMessageSender().onError(e, "modrinth", context, false);
            }
        });
        return true;
    }

    private static void displayFiles(String key, MessageContext context, Environment environment) throws IOException {
        JsonArray addons = search(key, 1, 0);
        if (addons.size() == 0)
            throw new IOException("未查找到此模组，请矫正拼写");

        JsonObject mod = addons.get(0).getAsJsonObject();
        String modName = JsonUtil.getStringOrNull(mod, "title");
        StringBuilder builder = new StringBuilder();
        if (!modName.equalsIgnoreCase(key))
            builder.append("未能查找到名称相同的模组，自动从 ").append(key).append(" 重定向到最高匹配项 ").append(modName).append("\n");

        JsonArray gameVersionLatestFiles = mod.getAsJsonArray("versions");
        List<String> versions = new ArrayList<>();
        List<String> finalVersions = versions;
        gameVersionLatestFiles.forEach(jsonElement -> finalVersions.add(jsonElement.getAsString()));
        if (versions.size() > 25) {
            versions = versions.stream().filter(en -> RegexUtil.match(MINECRAFT_VERSION, en)).collect(Collectors.toList());
            builder.append("(仅显示正式版)");
        }

        String slug = JsonUtil.getStringOrNull(mod, "slug");
        if (versions.size() > 15)
            builder.append("(仅显示前15文件)\n");
        for (int i = versions.size() - 1, j = 0; j < 15 && i >= 0; i--, j++) {
            JsonElement element = WebUtil.fetchDataInJson(new HttpGet(MODRINTH_API_URL + "/project/" + slug
                    + "/version?game_versions=" + WebUtil.encode("[\"" + versions.get(i) + "\"]")));
            JsonArray array = element.getAsJsonArray();
            JsonObject object = array.get(0).getAsJsonObject();
            builder.append(versions.get(i)).append(": ").append(
                    JsonUtil.getStringInPathOrNull(object, "files.0.url")).append("\n");
        }

        environment.getMessageSender().sendMessageRecallable(context, environment.newChain(
                environment.newQuote(context.message()),
                environment.newText(builder.toString().trim())
        ));
    }

    private static void displaySearch(String key, MessageContext context, Environment environment) throws IOException {
        int page = 0;
        int pageSize = 10;
        if (RegexUtil.match(SEARCH_PAGE_PATTERN, key)) {
            String[] split = key.split(":", 2);
            key = split[1];
            String[] regions = split[0].split(",");
            if (!regions[0].isEmpty())
                page = Integer.parseInt(regions[0]);
            if (regions.length > 1 && !regions[1].isEmpty())
                pageSize = Math.min(30, Integer.parseInt(regions[1]));
        }

        JsonArray addons = search(key, pageSize, page * pageSize);
        if (addons.size() == 0)
            throw new IOException("未查找到此模组，请矫正拼写");

        StringBuilder builder = new StringBuilder("查找结果\n");
        builder.append("<最多显示").append(pageSize).append("项>");
        if (page > 0)
            builder.append("(第").append(page).append("页，第").append(page * pageSize).append("项结果开始)");
        builder.append("\n");
        for (JsonElement element : addons) {
            JsonObject object = element.getAsJsonObject();
            String name = JsonUtil.getStringOrNull(object, "title");
            String summary = JsonUtil.getStringOrNull(object, "description");
            if (summary.length() > 63)
                summary = summary.substring(0, 60) + "...";
            builder.append("[").append(name).append("]").append(summary).append("\n");
        }

        environment.getMessageSender().sendMessageRecallable(context, environment.newChain(
                environment.newQuote(context.message()),
                environment.newText(builder.toString().trim())
        ));
    }

    private static void displayMod(String id, MessageContext context, Environment environment) throws IOException {
        // Guess the first mod
        JsonArray addons = search(id, 1, 0);
        if (addons.size() == 0)
            throw new IOException("未查找到此模组，请矫正拼写");

        JsonObject mod = addons.get(0).getAsJsonObject();
        String modName = JsonUtil.getStringOrNull(mod, "title");
        StringBuilder builder = new StringBuilder();
        if (!modName.equalsIgnoreCase(id))
            builder.append("未能查找到名称相同的模组，自动从 ").append(id).append(" 重定向到最高匹配项 ").append(modName).append("\n");
        builder.append("模组名称: ").append(modName).append("\n");

        JsonArray categoriesArray = mod.getAsJsonArray("categories");
        if (categoriesArray != null && categoriesArray.size() > 0) {
            builder.append("分类: ");
            for (JsonElement element : categoriesArray)
                builder.append(element.getAsString()).append(", ");
            builder.delete(builder.length() - 2, builder.length());
            builder.append("\n");
        }

        builder.append("作者: ").append(JsonUtil.getStringOrNull(mod, "author")).append("\n");

        int downloadCount = JsonUtil.getIntOrZero(mod, "downloads");
        builder.append("下载量: ").append(downloadCount).append("\n");

        builder.append("支持版本: ");
        JsonArray gameVersionLatestFiles = mod.getAsJsonArray("versions");
        List<String> versions = new ArrayList<>();
        List<String> finalVersions = versions;
        gameVersionLatestFiles.forEach(jsonElement -> finalVersions.add(jsonElement.getAsString()));
        if (versions.size() > 25) {
            versions = versions.stream().filter(en -> RegexUtil.match(MINECRAFT_VERSION, en)).collect(Collectors.toList());
            builder.append("(仅显示正式版)");
        }
        versions.forEach(version -> builder.append(version).append(", "));
        builder.delete(builder.length() - 2, builder.length());
        builder.append("\n");

        BufferedReader reader = new BufferedReader(new StringReader(JsonUtil.getStringOrNull(mod, "description")));
        String line;
        while ((line = reader.readLine()) != null && builder.length() <= 801) {
            line = line.trim();
            if (!line.isEmpty())
                builder.append(line).append("\n");
        }
        if (line != null)
            builder.append("(原文过长截断，完整信息请访问主条目URL)");

        environment.getMessageSender().sendMessageRecallable(context, environment.newChain(
                environment.newQuote(context.message()),
                environment.newText(builder.toString().trim())
        ));
    }

    private static JsonArray search(String key, int limit, int offset) throws IOException {
        String filter = null;
        String[] keySplit = key.split("\\|", 2);
        if (keySplit.length == 2) {
            key = keySplit[0];
            filter = keySplit[1];
        }

        JsonElement e = WebUtil.fetchDataInJson(new HttpGet(MODRINTH_API_URL + "/search?query="
                + WebUtil.encode(key) + "&offset=" + offset + "&limit=" + limit
                + (filter == null ? "" : ("&filters=" + WebUtil.encode(filter)))
        ));

        if (e == null || e instanceof JsonNull)
            throw new IOException("未查找到此模组，请矫正拼写");
        JsonObject addonIds = e.getAsJsonObject();
        if (addonIds.has("error"))
            throw new IOException(addonIds.get("description").getAsString());

        return addonIds.getAsJsonArray("hits");
    }
}
