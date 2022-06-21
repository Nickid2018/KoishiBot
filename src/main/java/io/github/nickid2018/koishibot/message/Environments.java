package io.github.nickid2018.koishibot.message;

import io.github.nickid2018.koishibot.message.api.Environment;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Environments {

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


}
