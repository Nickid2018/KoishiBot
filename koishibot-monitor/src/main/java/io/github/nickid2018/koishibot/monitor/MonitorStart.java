package io.github.nickid2018.koishibot.monitor;

import io.github.nickid2018.koishibot.network.KoishiBotServer;
import io.github.nickid2018.koishibot.util.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MonitorStart {

    public static KoishiBotServer server;

    public static final Logger LOGGER = LoggerFactory.getLogger("Monitor");

    public static void main(String[] args) {
        try {
            Settings.loadSettings();
        } catch (Exception e) {
            LogUtils.error(LOGGER, "Failed to load settings", e);
            return;
        }
        if (!EnvironmentCheck.checkAndCreate()) {
            LogUtils.info(LogUtils.FontColor.GREEN, LOGGER,
                    "Created a bot environment, please configure it correctly and restart the monitor.");
            return;
        }
    }
}
