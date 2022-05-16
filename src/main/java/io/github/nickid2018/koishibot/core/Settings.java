package io.github.nickid2018.koishibot.core;

import com.google.gson.*;
import io.github.nickid2018.koishibot.KoishiBotMain;
import io.github.nickid2018.koishibot.wiki.WikiInfo;
import org.apache.commons.io.IOUtils;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Settings {

    public static long BOT_QQ;
    public static String BOT_PASSWORD;

    public static String YOUDAO_APP_KEY;
    public static String YOUDAO_APP_SECRET;

    public static final Map<String, WikiInfo> SUPPORT_WIKIS = new HashMap<>();
    public static final Set<String> HIDE_WIKIS = new HashSet<>();
    public static String BASE_WIKI;

    public static void load() throws IOException {
        String data = IOUtils.toString(new FileReader(KoishiBotMain.INSTANCE.resolveConfigFile("botKoishi.json")));
        JsonObject settingsRoot = JsonParser.parseString(data).getAsJsonObject();

        BOT_QQ = settingsRoot.get("qq").getAsLong();
        BOT_PASSWORD = settingsRoot.get("password").getAsString();

        loadWiki(settingsRoot);
        loadAppKeyAndSecrets(settingsRoot);
    }

    public static void reload() throws IOException {
        String data = IOUtils.toString(new FileReader(
                KoishiBotMain.INSTANCE.resolveConfigFile("botKoishi.json")));
        JsonObject settingsRoot = JsonParser.parseString(data).getAsJsonObject();

        loadWiki(settingsRoot);
    }

    public static void loadWiki(JsonObject settingsRoot) {
        JsonObject wikiRoot = settingsRoot.getAsJsonObject("wiki");
        JsonObject wikisArray = wikiRoot.getAsJsonObject("wikis");
        for (Map.Entry<String, JsonElement> en : wikisArray.entrySet())
            if (!SUPPORT_WIKIS.containsKey(en.getKey())) {
                if (en.getValue() instanceof JsonPrimitive)
                    SUPPORT_WIKIS.put(en.getKey(), new WikiInfo(en.getValue().getAsString() + "?"));
                else {
                    JsonObject wikiData = en.getValue().getAsJsonObject();
                    JsonObject headers = wikiData.getAsJsonObject("headers");
                    Map<String, String> header = new HashMap<>();
                    for (Map.Entry<String, JsonElement> headerEntry : headers.entrySet())
                        header.put(headerEntry.getKey(), headerEntry.getValue().getAsString());
                    SUPPORT_WIKIS.put(en.getKey(), new WikiInfo(wikiData.get("url").getAsString() + "?", header));
                }
            }
        JsonArray hideWikis = wikiRoot.getAsJsonArray("hides");
        for (JsonElement element : hideWikis)
            HIDE_WIKIS.add(element.getAsString());
        BASE_WIKI = wikiRoot.get("base").getAsString();
    }

    public static void loadAppKeyAndSecrets(JsonObject settingsRoot) {
        JsonObject youdao = settingsRoot.getAsJsonObject("youdao");
        YOUDAO_APP_KEY = youdao.get("app_key").getAsString();
        YOUDAO_APP_SECRET = youdao.get("app_secret").getAsString();
    }
}
