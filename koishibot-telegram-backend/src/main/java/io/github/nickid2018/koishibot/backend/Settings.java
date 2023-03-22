package io.github.nickid2018.koishibot.backend;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.nickid2018.koishibot.util.JsonUtil;
import org.apache.commons.io.IOUtils;

import java.io.FileReader;
import java.io.IOException;

public class Settings {

    public static final String CONFIG_FILE = "bot_telegram.json";

    public static String uid;
    public static String token;
    public static int delegatePort;

    public static String proxyHost;
    public static int proxyPort;
    public static String proxyType;

    public static void loadSettings() throws IOException {
        JsonObject config = JsonParser.parseString(IOUtils.toString(new FileReader(CONFIG_FILE))).getAsJsonObject();
        uid = JsonUtil.getStringOrNull(config, "uid");
        token = JsonUtil.getStringOrNull(config, "token");
        delegatePort = JsonUtil.getIntOrElse(config, "delegate_port", 52514);
        JsonUtil.getString(config, "proxy_host").ifPresent(host -> {
            proxyHost = host;
            proxyPort = JsonUtil.getIntOrElse(config, "proxy_port", 7890);
            proxyType = JsonUtil.getStringOrElse(config, "proxy_type", "http");
        });
    }
}
