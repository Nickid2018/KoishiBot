package io.github.nickid2018.koishibot.core;

import io.github.nickid2018.koishibot.message.Environments;
import io.github.nickid2018.koishibot.util.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Locale;
import java.util.Scanner;

public class BotStart {

    public static final long START_TIME = System.currentTimeMillis();
    public static final Logger LOGGER = LoggerFactory.getLogger("Bot Core");

    public static void main(String[] args) {
        try {
            start();
        } catch (Exception e) {
            LogUtils.error(LOGGER, "Failed to start bot", e);
            return;
        }
        Scanner scanner = new Scanner(System.in);
        MAIN: while (scanner.hasNext()) {
            String command = scanner.nextLine();
            switch (command.toLowerCase(Locale.ROOT)) {
                case "reload":
                    try {
                        Settings.reload();
                    } catch (IOException e) {
                        LogUtils.error(LOGGER, "Failed to reload settings", e);
                    }
                    break;
                case "stop":
                    break MAIN;
            }
        }
        terminate();
    }

    private static void start() throws IOException {
        MonitorListener.startLink();
        PluginProcessor.initProcess();
        Settings.load();
        Environments.startServer();
        PluginProcessor.init();
    }

    public static void terminate() {
        PluginProcessor.exit();
        Environments.closeEnvironments();
        System.exit(0);
    }
}
