package io.github.nickid2018.koishibot.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
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

    public static final Map<String ,BotLoginData> BOT_DATA = new HashMap<>();

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

    public static String GITHUB_OAUTH2_CLIENT_ID;
    public static String GITHUB_OAUTH2_CLIENT_SECRET;

    public static void load() throws IOException {
        String data = IOUtils.toString(new FileReader("botKoishi.json"));
        JsonObject settingsRoot = JsonParser.parseString(data).getAsJsonObject();

        JsonObject bots = settingsRoot.getAsJsonObject("bots");
        for (Map.Entry<String, JsonElement> en : bots.entrySet()) {
            JsonObject loginData = en.getValue().getAsJsonObject();
            BOT_DATA.put(en.getKey(), new BotLoginData(
                    JsonUtil.getStringOrNull(loginData, "uid"), JsonUtil.getStringOrNull(loginData, "password")));
        }

        LOCAL_IP = JsonUtil.getStringOrNull(settingsRoot, "local_ip");

        loadInternal(settingsRoot);
    }

    public static void reload() throws IOException {
        String data = IOUtils.toString(new FileReader("botKoishi.json"));
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
                    SUPPORT_WIKIS.put(en.getKey(), new WikiInfo(JsonUtil.getStringOrNull(wikiData, "url") + "?", header));
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
        YOUDAO_APP_KEY = JsonUtil.getStringOrNull(youdao, "app_key");
        YOUDAO_APP_SECRET = JsonUtil.getStringOrNull(youdao, "app_secret");
    }

    public static void loadFFmpeg(JsonObject settingsRoot) {
        JsonObject audio = settingsRoot.getAsJsonObject("audio");
        FFMPEG_LOCATION = JsonUtil.getStringOrNull(audio, "ffmpeg");
        ENCODER_LOCATION = JsonUtil.getStringOrNull(audio, "encoder");
    }

    public static void loadImageSettings(JsonObject settingsRoot) {
        JsonObject image = settingsRoot.getAsJsonObject("image");
        IMAGE_FONT = new Font(JsonUtil.getStringOrNull(image, "family"), Font.PLAIN,
                JsonUtil.getIntOrZero(image, "size"));
        IMAGE_FONT_BOLD = new Font(JsonUtil.getStringOrNull(image, "family"), Font.BOLD,
                JsonUtil.getIntOrZero(image, "size"));
    }

    public static void loadSensitiveWordsSettings(JsonObject settingsRoot) {
        JsonUtil.getString(settingsRoot, "sensitives").ifPresent(
                s -> {
                    try {
                        SensitiveWordFilter.loadWordFromFile(s);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    public static void loadWebDriver(JsonObject settingsRoot) {
        InfoBoxShooter.close();
        JsonUtil.getString(settingsRoot, "webdriver").ifPresent(web -> {
            System.setProperty("webdriver.gecko.driver", web);
            InfoBoxShooter.loadWebDriver();
        });
    }

    public static void loadGitHub(JsonObject settingsRoot) {
        GITHUB_TOKEN = JsonUtil.getString(settingsRoot, "github_token").orElse("");
        JsonUtil.getData(settingsRoot, "github_oauth2", JsonObject.class).ifPresent(oauth -> {
            GITHUB_OAUTH2_CLIENT_ID = JsonUtil.getStringOrNull(oauth, "client_id");
            GITHUB_OAUTH2_CLIENT_SECRET = JsonUtil.getStringOrNull(oauth, "client_secret");
        });
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

        JsonUtil.getData(settingsRoot, "proxy", JsonObject.class).ifPresent(root -> {
                    String type = JsonUtil.getData(root, "type", JsonPrimitive.class)
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
        );
    }
}
