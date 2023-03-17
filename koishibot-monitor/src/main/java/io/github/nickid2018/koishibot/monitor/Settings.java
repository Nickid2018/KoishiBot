package io.github.nickid2018.koishibot.monitor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import io.github.nickid2018.koishibot.util.JsonUtil;
import org.apache.commons.io.IOUtils;

import java.io.FileReader;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Optional;

public class Settings {

    public static int CORE_PORT;
    public static String GITHUB_TOKEN;
    public static String ACTION_REPO;
    public static int PROXY_PORT = -1;
    public static String PROXY_HOST = null;
    public static String PROXY_TYPE = null;
    public static String[] ENABLE_BACKENDS;

    public static void loadSettings() throws Exception {
        JsonObject settings = JsonParser.parseString(IOUtils.toString(new FileReader("monitor-settings.json"))).getAsJsonObject();
        CORE_PORT = JsonUtil.getIntOrElse(settings, "core_port", 23333);
        GITHUB_TOKEN = JsonUtil.getStringOrNull(settings, "github_token");
        ACTION_REPO = JsonUtil.getStringOrElse(settings, "action_repo", "Nickid2018/KoishiBot;build.yml");
        ENABLE_BACKENDS = JsonUtil.getString(settings, "enable_backends").map(s -> s.split(",")).orElse(new String[0]);
        loadProxy(settings);
    }

    public static void loadProxy(JsonObject settingsRoot) {
        System.setProperty("java.net.useSystemProxies", "true");
        Authenticator.setDefault(null);
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
                        MonitorStart.LOGGER.info("Set proxy, type = http, host = {}, port = {}", host, port.orElse(7890));
                    } else {
                        System.setProperty("socksProxyHost", PROXY_HOST = host);
                        System.setProperty("socksProxyPort", String.valueOf(PROXY_PORT = port.orElse(1080)));
                        MonitorStart.LOGGER.info("Set proxy, type = socks, host = {}, port = {}", host, port.orElse(1080));
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
