package io.github.nickid2018.koishibot.message;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Environments {

    private static final Map<String, DelegateEnvironment> ENVIRONMENT_MAP = new HashMap<>();

    public static void putEnvironment(String id, DelegateEnvironment environment) {
        ENVIRONMENT_MAP.put(id, environment);
    }

    public static DelegateEnvironment getEnvironment(String id) {
        return ENVIRONMENT_MAP.get(id);
    }

    public static Set<String> getEnvironmentIDs() {
        return ENVIRONMENT_MAP.keySet();
    }

    public static Collection<DelegateEnvironment> getEnvironments() {
        return ENVIRONMENT_MAP.values();
    }

    public static void addEnvironment(DelegateEnvironment environment) {
        ENVIRONMENT_MAP.put(environment.getEnvironmentName(), environment);
    }

    public static void removeEnvironment(String id) {
        ENVIRONMENT_MAP.remove(id);
    }

    public static void closeEnvironments() {
        ENVIRONMENT_MAP.values().forEach(DelegateEnvironment::close);
    }
}
