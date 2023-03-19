package io.github.nickid2018.koishibot.monitor;

import io.github.nickid2018.koishibot.network.KoishiBotServer;
import io.github.nickid2018.koishibot.util.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

public class MonitorStart {

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
        KoishiBotServer server = new KoishiBotServer(Settings.CORE_PORT, CoreListener.REGISTRY, new CoreListener());
        server.start(30);
        LogUtils.info(LogUtils.FontColor.GREEN, LOGGER, "Core server started");
        try {
            ProcessManager.startCore();
            for (String backend : Settings.ENABLE_BACKENDS)
                ProcessManager.startBackend(backend);
            LogUtils.info(LogUtils.FontColor.GREEN, LOGGER, "Automatically switch to core process");
            ProcessManager.setInteract("core");
        } catch (IOException e) {
            LogUtils.error(LOGGER, "Failed to start core or backends", e);
        }
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String line = scanner.nextLine();
            try {
                if (commandExitProcess(line) && !ProcessManager.sendCommand(line))
                    builtinCommand(line);
            } catch (IOException e) {
                LogUtils.error(LOGGER, "Failed to send command", e);
            }
        }
    }

    private static boolean commandExitProcess(String line) {
        if (line.equalsIgnoreCase("quit") && ProcessManager.nowInteract()) {
            LogUtils.info(LogUtils.FontColor.GREEN, LOGGER, "-----Exit the process-----");
            ProcessManager.setNoInteract();
            return false;
        }
        return true;
    }

    private static void builtinCommand(String line) {
        String[] args = line.split(" ");
        switch (args[0].toLowerCase()) {
            case "enter" -> {
                if (args.length != 2)
                    LogUtils.error(LOGGER, "Invalid arguments", null);
                else if (!ProcessManager.setInteract(args[1]))
                    LogUtils.error(LOGGER, "Invalid sub-process name", null);
                else
                    LogUtils.info(LogUtils.FontColor.GREEN, LOGGER, "-----Enter the process-----");
            }
            case "check" -> {
                if (args.length != 2)
                    LogUtils.error(LOGGER, "Invalid arguments", null);
                else if (ProcessManager.nowRunning().contains(args[1]))
                    LogUtils.info(LogUtils.FontColor.GREEN, LOGGER, "Sub-process {} is running", args[1]);
                else {
                    try {
                        if (args[1].equals("core")) {
                            ProcessManager.startCore();
                            LogUtils.info(LogUtils.FontColor.GREEN, LOGGER, "Started core");
                        } else if (Arrays.stream(Settings.ENABLE_BACKENDS).anyMatch(s -> s.equalsIgnoreCase(args[1]))) {
                            ProcessManager.startBackend(args[1]);
                            LogUtils.info(LogUtils.FontColor.GREEN, LOGGER, "Started backend {}", args[1]);
                        } else
                            LogUtils.error(LOGGER, "Invalid sub-process name", null);
                    } catch (Exception e) {
                        LogUtils.error(LOGGER, "Can't start sub-process", e);
                    }
                }
            }
            case "exit" -> {

            }
        }
    }
}
