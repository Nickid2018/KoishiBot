package io.github.nickid2018.koishibot.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import io.github.nickid2018.koishibot.util.JsonUtil;
import io.github.nickid2018.koishibot.util.ReflectTarget;
import org.apache.commons.io.IOUtils;

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

        LOCAL_IP = JsonUtil.getStringOrNull(settingsRoot, "local_ip");

        System.err.close();
        PluginProcessor.set(settingsRoot);
    }

    public static void reload() throws IOException {
        String data = IOUtils.toString(new FileReader("botKoishi.json"));
        JsonObject settingsRoot = JsonParser.parseString(data).getAsJsonObject();

        PluginProcessor.set(settingsRoot);
    }

    @ReflectTarget
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
