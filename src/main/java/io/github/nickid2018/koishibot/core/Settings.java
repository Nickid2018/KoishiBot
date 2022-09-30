package io.github.nickid2018.koishibot.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import io.github.nickid2018.koishibot.util.JsonUtil;
import org.apache.commons.io.IOUtils;

import java.io.FileReader;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Settings {

    public static final Map<String ,BotLoginData> BOT_DATA = new HashMap<>();

    public static String LOCAL_IP;
    public static int OPEN_PORT;

    public static String PROXY_HOST;
    public static int PROXY_PORT;
    public static String PROXY_TYPE;

    public static void load() throws IOException {
        String data = IOUtils.toString(new FileReader("botKoishi.json"));
        JsonObject settingsRoot = JsonParser.parseString(data).getAsJsonObject();

        JsonObject bots = settingsRoot.getAsJsonObject("bots");
        for (Map.Entry<String, JsonElement> en : bots.entrySet()) {
            JsonObject loginData = en.getValue().getAsJsonObject();
            BOT_DATA.put(en.getKey(), new BotLoginData(
                    JsonUtil.getStringOrNull(loginData, "uid"),
                    JsonUtil.getStringOrNull(loginData, "password"),
                    JsonUtil.getStringOrNull(loginData, "token")));
        }

        LOCAL_IP = JsonUtil.getStringOrElse(settingsRoot, "local_ip", "localhost");
        OPEN_PORT = JsonUtil.getIntOrElse(settingsRoot, "port", -1);

        System.err.close();
        PluginProcessor.set(settingsRoot);
    }

    public static void reload() throws IOException {
        String data = IOUtils.toString(new FileReader("botKoishi.json"));
        JsonObject settingsRoot = JsonParser.parseString(data).getAsJsonObject();

        PluginProcessor.set(settingsRoot);
    }

    public static void loadProxy(JsonObject settingsRoot) {
        System.setProperty("java.net.useSystemProxies", "true");
        Authenticator.setDefault(null);
        PROXY_PORT = -1;
        PROXY_HOST = null;
        PROXY_TYPE = null;
        JsonUtil.getData(settingsRoot, "proxy", JsonObject.class).ifPresent(root -> {
                    String type = JsonUtil.getData(root, "type", JsonPrimitive.class)
                            .map(JsonPrimitive::getAsString)
                            .filter(s -> s.equalsIgnoreCase("http")
                                    || s.equalsIgnoreCase("socks"))
                            .orElse("http");

                    String host = JsonUtil.getStringInPathOrElse(root, "host", "127.0.0.1");
                    Optional<Integer> port = JsonUtil.getDataInPath(root, "port", JsonPrimitive.class)
                            .filter(JsonPrimitive::isNumber)
                            .map(JsonPrimitive::getAsInt);

                    PROXY_TYPE = type;
                    if (type.equalsIgnoreCase("http")) {
                        System.setProperty("http.proxyHost", PROXY_HOST = host);
                        System.setProperty("http.proxyPort", String.valueOf(PROXY_PORT = port.orElse(7890)));
                        PluginProcessor.LOGGER.info("Set proxy, type = http, host = {}, port = {}", host, port.orElse(7890));
                    } else {
                        System.setProperty("socksProxyHost", PROXY_HOST = host);
                        System.setProperty("socksProxyPort", String.valueOf(PROXY_PORT = port.orElse(1080)));
                        PluginProcessor.LOGGER.info("Set proxy, type = socks, host = {}, port = {}", host, port.orElse(1080));
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
