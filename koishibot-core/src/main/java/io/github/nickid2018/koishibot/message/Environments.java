package io.github.nickid2018.koishibot.message;

import io.github.nickid2018.koishibot.core.Settings;
import io.github.nickid2018.koishibot.network.Connection;
import io.github.nickid2018.koishibot.network.KoishiBotServer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Environments {

    private static final Map<String, DelegateEnvironment> ENVIRONMENT_MAP = new HashMap<>();
    private static final Map<Connection, DelegateEnvironment> ENVIRONMENT_CONNECTION_MAP = new HashMap<>();
    private static KoishiBotServer server;

    public static void startServer() {
        server = new KoishiBotServer(Settings.DELEGATE_PORT, MessageDataListener.REGISTRY, new MessageDataListener());
        server.start(30);
    }

    public static void putEnvironment(String id, DelegateEnvironment environment) {
        ENVIRONMENT_MAP.put(id, environment);
        ENVIRONMENT_CONNECTION_MAP.put(environment.getConnection(), environment);
    }

    public static DelegateEnvironment getEnvironment(String id) {
        server.checkConnections();
        return ENVIRONMENT_MAP.get(id);
    }

    public static DelegateEnvironment getEnvironment(Connection connection) {
        return ENVIRONMENT_CONNECTION_MAP.get(connection);
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
        DelegateEnvironment environment = ENVIRONMENT_MAP.remove(id);
        ENVIRONMENT_CONNECTION_MAP.remove(environment.getConnection());
    }

    public static void removeEnvironment(DelegateEnvironment environment) {
        ENVIRONMENT_MAP.remove(environment.getEnvironmentName());
        ENVIRONMENT_CONNECTION_MAP.remove(environment.getConnection());
    }

    public static void closeEnvironments() {
        ENVIRONMENT_MAP.values().forEach(DelegateEnvironment::close);
        ENVIRONMENT_MAP.clear();
        ENVIRONMENT_CONNECTION_MAP.clear();
        server.stop();
    }
}
