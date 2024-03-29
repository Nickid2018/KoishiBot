package io.github.nickid2018.koishibot.message;

import io.github.nickid2018.koishibot.core.BotStart;
import io.github.nickid2018.koishibot.core.Settings;
import io.github.nickid2018.koishibot.network.Connection;
import io.github.nickid2018.koishibot.network.KoishiBotServer;
import io.github.nickid2018.koishibot.util.LogUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Environments {

    private static final Map<String, DelegateEnvironment> ENVIRONMENT_MAP = new HashMap<>();
    private static final Map<Connection, DelegateEnvironment> ENVIRONMENT_CONNECTION_MAP = new HashMap<>();
    private static KoishiBotServer server;

    public static void startServer() {
        MessageDataListener listener = new MessageDataListener();
        server = new KoishiBotServer(Settings.DELEGATE_PORT, listener.getRegistry(), listener);
        server.start();
        LogUtils.info(LogUtils.FontColor.CYAN, BotStart.LOGGER, "Delegate server started");
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
