package io.github.nickid2018.koishibot.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class CreateAStart {

    public static final Logger LOGGER = LoggerFactory.getLogger("Creating Process");
    public static final File MONITOR_DATA_FILE = new File("monitor_data.json");

    public static boolean checkAndCreate() {
        if (!MONITOR_DATA_FILE.isFile()) {
            create();
            return false;
        } else
            return true;
    }

    private static void create() {
        LOGGER.info("Creating a bot environment...");
    }
}
