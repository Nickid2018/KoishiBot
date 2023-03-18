package io.github.nickid2018.koishibot.monitor;

import io.github.nickid2018.koishibot.util.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
        try {
            ProcessManager.startCore();
            ProcessManager.setInteract("core");
            for (String backend : Settings.ENABLE_BACKENDS)
                ProcessManager.startBackend(backend);
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
            case "exit" -> {

            }
        }
    }
}
