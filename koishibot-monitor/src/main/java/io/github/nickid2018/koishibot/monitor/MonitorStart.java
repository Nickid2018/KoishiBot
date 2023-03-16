package io.github.nickid2018.koishibot.monitor;

import io.github.nickid2018.koishibot.network.KoishiBotServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MonitorStart {

    public static KoishiBotServer server;

    public static final Logger LOGGER = LoggerFactory.getLogger("Monitor");

    public static void main(String[] args) {
        try {
            Settings.loadSettings();
        } catch (IOException e) {
            LOGGER.error("Failed to load settings", e);
            return;
        }
        if (!CreateAStart.checkAndCreate()) {
            LOGGER.info("Created a bot environment, please configure it correctly and restart the monitor.");
            return;
        }
    }
}
