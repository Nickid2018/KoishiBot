package io.github.nickid2018.koishibot.backend;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.nickid2018.koishibot.util.JsonUtil;
import org.apache.commons.io.IOUtils;

import java.io.FileReader;
import java.io.IOException;

public class Settings {

    public static final String CONFIG_FILE = "bot_qq.json";

    public static long id;
    public static String password;
    public static String protocol;
    public static int delegatePort;

    public static void loadSettings() throws IOException {
        JsonObject config = JsonParser.parseString(IOUtils.toString(new FileReader(CONFIG_FILE))).getAsJsonObject();
        String strID = JsonUtil.getStringOrNull(config, "id");
        if (strID == null)
            throw new IllegalArgumentException("Bot ID is not set.");
        id = Long.parseLong(strID);
        password = JsonUtil.getStringOrNull(config, "password");
        if (password == null)
            throw new IllegalArgumentException("Bot password is not set.");
        protocol = JsonUtil.getStringOrNull(config, "protocol");

        delegatePort = JsonUtil.getIntOrElse(config, "delegate_port", 52514);
    }
}
