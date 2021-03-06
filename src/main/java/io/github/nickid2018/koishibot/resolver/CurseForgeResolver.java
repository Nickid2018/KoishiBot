package io.github.nickid2018.koishibot.resolver;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.util.AsyncUtil;
import io.github.nickid2018.koishibot.util.JsonUtil;
import io.github.nickid2018.koishibot.util.RegexUtil;
import io.github.nickid2018.koishibot.util.WebUtil;
import org.apache.http.client.methods.HttpGet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CurseForgeResolver extends MessageResolver {

    public static final Pattern MOD_PATTERN = Pattern.compile("<cur:.+?>");
    public static final Pattern MOD_FILES_PATTERN = Pattern.compile("<cur:files:.+?>");
    public static final Pattern MINECRAFT_VERSION = Pattern.compile("1\\.\\d{1,2}(\\.\\d)?");

    public CurseForgeResolver() {
        super(MOD_FILES_PATTERN, MOD_PATTERN);
    }

    @Override
    public boolean resolveInternal(String key, MessageContext context, Object resolvedArguments, Environment environment) {
        AsyncUtil.execute(() -> {
            try {
                if (resolvedArguments == MOD_FILES_PATTERN)
                    displayFiles(key.substring(11, key.length() - 1), context, environment);
                else
                    displayMod(key.substring(5, key.length() - 1), context, environment);
            } catch (Exception e) {
                environment.getMessageSender().onError(e, "curseforge", context, false);
            }
        });
        return true;
    }

    private static void displayFiles(String key, MessageContext context, Environment environment) throws IOException {
        JsonObject mod = WebUtil.fetchDataInJson(new HttpGet("https://api.cfwidget.com/minecraft/mc-mods/"
                + URLEncoder.encode(key.replace(" ", "-"), StandardCharsets.UTF_8))).getAsJsonObject();

        StringBuilder builder = new StringBuilder();
        JsonObject gameVersions = mod.getAsJsonObject("versions");
        List<String> versions = new ArrayList<>(gameVersions.keySet());
        if (versions.size() > 25) {
            versions = versions.stream().filter(en -> RegexUtil.match(MINECRAFT_VERSION, en)).collect(Collectors.toList());
            builder.append("(??????????????????)\n");
        }

        versions.forEach(version -> {
            JsonObject object = gameVersions.getAsJsonObject(version);
            builder.append(version).append(": ");
            builder.append("https://mediafiles.forgecdn.net/files/");
            int id = JsonUtil.getIntOrZero(object, "id");
            builder.append(id / 1000).append("/").append(id % 1000).append("/");
            builder.append(JsonUtil.getStringOrNull(object, "name")).append("\n");
        });

        environment.getMessageSender().sendMessageRecallable(context, environment.newChain(
                environment.newQuote(context.message()),
                environment.newText(builder.toString().trim())
        ));
    }

    private static void displayMod(String id, MessageContext context, Environment environment) throws IOException {
        JsonObject mod = WebUtil.fetchDataInJson(new HttpGet("https://api.cfwidget.com/minecraft/mc-mods/"
                + URLEncoder.encode(id.replace(" ", "-"), StandardCharsets.UTF_8))).getAsJsonObject();

        String modName = JsonUtil.getStringOrNull(mod, "title");
        StringBuilder builder = new StringBuilder();
        builder.append("????????????: ").append(modName).append("\n");

        JsonArray categories = mod.getAsJsonArray("categories");
        if (categories != null && categories.size() > 0) {
            builder.append("??????: ");
            for (JsonElement element : categories)
                builder.append(element.getAsString()).append(", ");
            builder.delete(builder.length() - 2, builder.length());
            builder.append("\n");
        }

        JsonArray members = mod.getAsJsonArray("members");
        List<String> name = new ArrayList<>();
        for (JsonElement element : members)
            name.add(JsonUtil.getStringOrNull(element.getAsJsonObject(), "username"));
        builder.append("??????: ").append(String.join(", ", name)).append("\n");

        builder.append("?????????: ").append(JsonUtil.getIntInPathOrZero(mod, "downloads.total")).append("\n");

        builder.append("????????????: ");
        JsonObject gameVersions = mod.getAsJsonObject("versions");
        List<String> versions = new ArrayList<>(gameVersions.keySet());
        if (versions.size() > 25) {
            versions = versions.stream().filter(en -> RegexUtil.match(MINECRAFT_VERSION, en)).collect(Collectors.toList());
            builder.append("(??????????????????)");
        }
        builder.append(String.join(", ", versions)).append("\n");

        BufferedReader reader = new BufferedReader(new StringReader(JsonUtil.getStringOrNull(mod, "summary")));
        String line;
        while ((line = reader.readLine()) != null && builder.length() <= 801) {
            line = line.trim();
            if (!line.isEmpty())
                builder.append(line).append("\n");
        }
        if (line != null)
            builder.append("(???????????????????????????????????????????????????URL)");

        environment.getMessageSender().sendMessageRecallable(context, environment.newChain(
                environment.newQuote(context.message()),
                environment.newText(builder.toString().trim())
        ));
    }
}
