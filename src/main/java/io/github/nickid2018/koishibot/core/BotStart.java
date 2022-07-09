package io.github.nickid2018.koishibot.core;

import io.github.nickid2018.koishibot.github.GitHubListener;
import io.github.nickid2018.koishibot.message.Environments;
import io.github.nickid2018.koishibot.message.MemberFilter;
import io.github.nickid2018.koishibot.util.AsyncUtil;
import io.github.nickid2018.koishibot.server.ServerManager;
import io.github.nickid2018.koishibot.wiki.InfoBoxShooter;

import java.io.IOException;
import java.util.Locale;
import java.util.Scanner;

public class BotStart {

    public static final long START_TIME = System.currentTimeMillis();

    public static void main(String[] args) {
        try {
            start();
        } catch (Exception e) {
            System.err.println("Cannot start bot");
            e.printStackTrace();
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
                        e.printStackTrace();
                    }
                    break;
                case "stop":
                    break MAIN;
            }
        }
        terminate();
    }

    private static void start() throws IOException {
        Settings.load();
        AsyncUtil.start();
        Environments.loadEnvironments();
        MemberFilter.init();
        GitHubListener.clinit();
    }

    private static void terminate() {
        Environments.closeEnvironments();
        AsyncUtil.terminate();
        TempFileSystem.close();
        InfoBoxShooter.close();
        ServerManager.stop();
        System.exit(0);
    }
}
