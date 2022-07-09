package io.github.nickid2018.koishibot.message;

import io.github.nickid2018.koishibot.core.BotLoginData;
import io.github.nickid2018.koishibot.core.Settings;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.kook.KOOKEnvironment;
import io.github.nickid2018.koishibot.message.qq.QQEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class Environments {

    public static final Logger ENVIRONMENT_LOGGER = LoggerFactory.getLogger("Environment");

    public static final Map<String, Function<BotLoginData, ? extends Environment>> ENVIRONMENT_PROVIDER = new HashMap<>();
    private static final Map<String, Environment> ENVIRONMENT_MAP = new HashMap<>();

    public static void putEnvironment(String id, Environment environment) {
        ENVIRONMENT_MAP.put(id, environment);
    }

    public static Environment getEnvironment(String id) {
        return ENVIRONMENT_MAP.get(id);
    }

    public static Set<String> getEnvironmentIDs() {
        return ENVIRONMENT_MAP.keySet();
    }

    public static Collection<Environment> getEnvironments() {
        return ENVIRONMENT_MAP.values();
    }

    public static void loadEnvironments() {
        Map<String, BotLoginData> loginDataMap = Settings.BOT_DATA;

        for (Map.Entry<String, BotLoginData> loginDataEntry : loginDataMap.entrySet()) {
            String backend = loginDataEntry.getKey();
            if (ENVIRONMENT_PROVIDER.containsKey(backend)) {
                ENVIRONMENT_MAP.put(backend, ENVIRONMENT_PROVIDER.get(backend).apply(loginDataEntry.getValue()));
                ENVIRONMENT_LOGGER.info("Successfully load environment {}.", backend);
            } else
                ENVIRONMENT_LOGGER.error("Backend Environment \"{}\" not found.", backend);
        }
        loginDataMap.clear();
    }

    public static void closeEnvironments() {
        ENVIRONMENT_MAP.values().forEach(Environment::close);
    }

    static {
        ENVIRONMENT_PROVIDER.put("qq", QQEnvironment::new);
        ENVIRONMENT_PROVIDER.put("kook", KOOKEnvironment::new);
    }
}
