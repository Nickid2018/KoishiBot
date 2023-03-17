package io.github.nickid2018.koishibot.monitor;

import io.github.nickid2018.koishibot.util.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class CreateAStart {

    public static final Logger LOGGER = LoggerFactory.getLogger("Creating Process");
    public static final File MONITOR_DATA_FILE = new File("monitor_data.json");

    public static boolean checkAndCreate() {
        if (!MONITOR_DATA_FILE.isFile()) {
            try {
                create();
            } catch (Exception e) {
                LogUtils.error(LOGGER, "Error when creating a bot environment", e);
                System.exit(-1);
            }
            return false;
        } else
            return true;
    }

    private static void create() throws Exception {
        LogUtils.info(LogUtils.FontColor.GREEN, LOGGER, "Creating a bot environment...");
        long actionID = GitHubWebRequests.getNowActionID();
        LogUtils.info(LogUtils.FontColor.CYAN, LOGGER, "Now action ID: {}", actionID);
        Object2LongMap<String> artifacts = GitHubWebRequests.getArtifacts(actionID);
        File coreFile = GitHubWebRequests.getArtifact(artifacts.getLong("core"));
        File coreLib = GitHubWebRequests.getArtifact(artifacts.getLong("core-libraries"));

    }
}
