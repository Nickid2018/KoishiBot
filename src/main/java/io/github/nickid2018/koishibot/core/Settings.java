package io.github.nickid2018.koishibot.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import io.github.nickid2018.koishibot.KoishiBotMain;
import io.github.nickid2018.koishibot.filter.SensitiveWordFilter;
import io.github.nickid2018.koishibot.util.JsonUtil;
import io.github.nickid2018.koishibot.wiki.InfoBoxShooter;
import io.github.nickid2018.koishibot.wiki.WikiInfo;
import org.apache.commons.io.IOUtils;

import java.awt.*;
import java.io.FileReader;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class Settings {

    public static long BOT_QQ;
    public static String BOT_PASSWORD;

    public static String LOCAL_IP;

    public static String YOUDAO_APP_KEY;
    public static String YOUDAO_APP_SECRET;

    public static String FFMPEG_LOCATION;
    public static String ENCODER_LOCATION;

    public static Font IMAGE_FONT;
    public static Font IMAGE_FONT_BOLD;

    public static final Map<String, WikiInfo> SUPPORT_WIKIS = new HashMap<>();
    public static final Map<String, String> MIRROR = new HashMap<>();
    public static String BASE_WIKI;

    public static String GITHUB_TOKEN;

    public static void load() throws IOException {
        String data = IOUtils.toString(new FileReader(KoishiBotMain.INSTANCE.resolveConfigFile("botKoishi.json")));
        JsonObject settingsRoot = JsonParser.parseString(data).getAsJsonObject();

        BOT_QQ = settingsRoot.get("qq").getAsLong();
        BOT_PASSWORD = settingsRoot.get("password").getAsString();
        LOCAL_IP = settingsRoot.get("local_ip").getAsString();

        loadInternal(settingsRoot);
    }

    public static void reload() throws IOException {
        String data = IOUtils.toString(new FileReader(
                KoishiBotMain.INSTANCE.resolveConfigFile("botKoishi.json")));
        JsonObject settingsRoot = JsonParser.parseString(data).getAsJsonObject();

        loadInternal(settingsRoot);
    }

    private static void loadInternal(JsonObject settingsRoot) throws IOException {
        loadWiki(settingsRoot);
        loadMirror(settingsRoot);
        loadFFmpeg(settingsRoot);
        loadAppKeyAndSecrets(settingsRoot);
        loadImageSettings(settingsRoot);
        loadWebDriver(settingsRoot);
        loadSensitiveWordsSettings(settingsRoot);
        loadGitHub(settingsRoot);
        loadProxy(settingsRoot);
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
        BASE_WIKI = wikiRoot.get("base").getAsString();
    }

    public static void loadMirror(JsonObject settingsRoot) {
        JsonObject wikisArray = settingsRoot.getAsJsonObject("mirrors");
        for (Map.Entry<String, JsonElement> en : wikisArray.entrySet())
            MIRROR.put(en.getKey(), en.getValue().getAsString());
    }

    public static void loadAppKeyAndSecrets(JsonObject settingsRoot) {
        JsonObject youdao = settingsRoot.getAsJsonObject("youdao");
        YOUDAO_APP_KEY = youdao.get("app_key").getAsString();
        YOUDAO_APP_SECRET = youdao.get("app_secret").getAsString();
    }

    public static void loadFFmpeg(JsonObject settingsRoot) {
        JsonObject audio = settingsRoot.getAsJsonObject("audio");
        FFMPEG_LOCATION = audio.get("ffmpeg").getAsString();
        ENCODER_LOCATION = audio.get("encoder").getAsString();
    }

    public static void loadImageSettings(JsonObject settingsRoot) {
        JsonObject image = settingsRoot.getAsJsonObject("image");
        IMAGE_FONT = new Font(image.get("family").getAsString(), Font.PLAIN,
                Integer.parseInt(image.get("size").getAsString()));
        IMAGE_FONT_BOLD = new Font(image.get("family").getAsString(), Font.BOLD,
                Integer.parseInt(image.get("size").getAsString()));
    }

    public static void loadSensitiveWordsSettings(JsonObject settingsRoot) throws IOException {
        if (settingsRoot.has("sensitives"))
            SensitiveWordFilter.loadWordFromFile(settingsRoot.get("sensitives").getAsString());
    }

    public static void loadWebDriver(JsonObject settingsRoot) {
        InfoBoxShooter.close();
        if (settingsRoot.has("webdriver")) {
            System.setProperty("webdriver.gecko.driver", settingsRoot.get("webdriver").getAsString());
            InfoBoxShooter.loadWebDriver();
        }
    }

    public static void loadGitHub(JsonObject settingsRoot) {
        JsonElement element = settingsRoot.get("github_token");
        if (element != null && element.isJsonPrimitive())
            GITHUB_TOKEN = element.getAsString();
        else
            GITHUB_TOKEN = "";
    }

    public static void loadProxy(JsonObject settingsRoot) {
        Properties properties = System.getProperties();
        properties.remove("http.proxyHost");
        properties.remove("http.proxyPort");
        properties.remove("https.proxyHost");
        properties.remove("https.proxyPort");
        properties.remove("http.nonProxyHosts");
        properties.remove("https.nonProxyHosts");
        properties.remove("socksProxyHost");
        properties.remove("socksProxyPort");
        Authenticator.setDefault(null);

        JsonElement element = settingsRoot.get("proxy");
        if (element != null && element.isJsonObject()) {
            JsonObject root = element.getAsJsonObject();

            String type = JsonUtil.getDataInPath(root, "type", JsonPrimitive.class)
                    .map(JsonPrimitive::getAsString)
                    .filter(s -> s.equalsIgnoreCase("http")
                            || s.equalsIgnoreCase("https")
                            || s.equalsIgnoreCase("socks"))
                    .orElse("http");

            String host = JsonUtil.getStringInPathOrElse(root, "host", "127.0.0.1");
            Optional<Integer> port = JsonUtil.getDataInPath(root, "port", JsonPrimitive.class)
                    .filter(JsonPrimitive::isNumber)
                    .map(JsonPrimitive::getAsInt);
            if (type.equalsIgnoreCase("http")) {
                properties.put("http.proxyHost", host);
                properties.put("http.proxyPort", port.orElse(80));
                properties.put("http.nonProxyHosts", "localhost");
            } else if (type.equalsIgnoreCase("https")) {
                properties.put("https.proxyHost", host);
                properties.put("https.proxyPort", port.orElse(443));
                properties.put("https.nonProxyHosts", "localhost");
            } else {
                properties.put("socksProxyHost", host);
                properties.put("socksProxyPort", port.orElse(1080));
            }

            JsonUtil.getDataInPath(root, "user", JsonPrimitive.class)
                    .map(JsonPrimitive::getAsString).ifPresent(user -> {
                        char[] password = JsonUtil.getDataInPath(root, "password", JsonPrimitive.class)
                                .map(JsonPrimitive::getAsString).map(String::toCharArray).orElse(new char[0]);
                        Authenticator.setDefault(new Authenticator() {
                            @Override
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(user, password);
                            }
                        });
                    });
        }
    }
}
