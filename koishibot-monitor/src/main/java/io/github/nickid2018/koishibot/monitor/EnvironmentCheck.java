package io.github.nickid2018.koishibot.monitor;

import io.github.nickid2018.koishibot.util.LogUtils;
import io.github.nickid2018.koishibot.util.Pair;
import io.github.nickid2018.koishibot.util.ZipUtil;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class EnvironmentCheck {

    public static final Logger LOGGER = LoggerFactory.getLogger("Creating Process");
    public static final File MONITOR_DATA_FILE = new File("monitor_data.json");

    public static boolean checkAndCreate() {
        if (!MONITOR_DATA_FILE.isFile()) {
            try {
                createFully();
            } catch (Exception e) {
                LogUtils.error(LOGGER, "Error when creating a bot environment", e);
                System.exit(-1);
            }
            return false;
        } else {
            try {
                long actionID = GitHubWebRequests.getNowActionID();
                LogUtils.info(LogUtils.FontColor.CYAN, LOGGER, "Now action ID: {}", actionID);
                Object2LongMap<String> artifacts = GitHubWebRequests.getArtifacts(actionID);
                File checksums = GitHubWebRequests.getArtifact(artifacts.getLong("checksums"));
                Pair<Set<String>, Set<String>> needUpdates = StatusCheck.needUpdates(checksums);
                Set<String> updateModules = new HashSet<>(needUpdates.first());
                updateModules.addAll(needUpdates.second());
                if (!needUpdates.first().isEmpty() || !needUpdates.second().isEmpty())
                    updateNotFully(artifacts, needUpdates.first(), needUpdates.second());
                StatusCheck.updateChecksums(checksums, updateModules);
            } catch (Exception e) {
                LogUtils.error(LOGGER, "Error when updating the bot environment", e);
                System.exit(-1);
            }
            return true;
        }
    }

    private static void createFully() throws Exception {
        LogUtils.info(LogUtils.FontColor.GREEN, LOGGER, "Creating a bot environment...");
        long actionID = GitHubWebRequests.getNowActionID();
        LogUtils.info(LogUtils.FontColor.CYAN, LOGGER, "Now action ID: {}", actionID);
        Object2LongMap<String> artifacts = GitHubWebRequests.getArtifacts(actionID);

        Set<String> installSuccessful = new HashSet<>();

        File checksums = GitHubWebRequests.getArtifact(artifacts.getLong("checksums"));
        try {
            File coreFile = GitHubWebRequests.getArtifact(artifacts.getLong("core"));
            File coreLib = GitHubWebRequests.getArtifact(artifacts.getLong("core-libraries"));
            ZipUtil.unzipTo(coreFile, new File("."));
            ZipUtil.unzipTo(coreLib, new File("libraries"));
            installSuccessful.add("core");
            LogUtils.info(LogUtils.FontColor.GREEN, LOGGER, "Installed core successfully");

            for (String backend : Settings.ENABLE_BACKENDS) {
                File core = GitHubWebRequests.getArtifact(artifacts.getLong(backend));
                File lib = GitHubWebRequests.getArtifact(artifacts.getLong(backend + "-libraries"));
                ZipUtil.unzipTo(core, new File(backend));
                ZipUtil.unzipTo(lib, new File(backend + "/libraries"));
                installSuccessful.add(backend);
                LogUtils.info(LogUtils.FontColor.GREEN, LOGGER, "Installed {} successfully", backend);
            }

            StatusCheck.updateChecksums(checksums, installSuccessful);
            LogUtils.info(LogUtils.FontColor.GREEN, LOGGER, "Updated checksums successfully");
        } catch (Exception e) {
            if (!installSuccessful.isEmpty())
                StatusCheck.updateChecksums(checksums, installSuccessful);
            throw e;
        }
    }

    public static void updateNotFully(Object2LongMap<String> artifacts,
                                      Set<String> backendCore, Set<String> backendLibs) throws Exception {
        for (String backend : backendCore) {
            File core = GitHubWebRequests.getArtifact(artifacts.getLong(backend));
            ZipUtil.unzipTo(core, new File(backend));
            LogUtils.info(LogUtils.FontColor.GREEN, LOGGER, "Updated {} successfully", backend);
        }
        for (String backendLib : backendLibs) {
            File lib = GitHubWebRequests.getArtifact(artifacts.getLong(backendLibs + "-libraries"));
            File toFile = new File(backendLib + "/libraries");
            if (toFile.isDirectory())
                Arrays.stream(Objects.requireNonNullElse(toFile.listFiles(), new File[0])).forEach(File::delete);
            ZipUtil.unzipTo(lib, toFile);
            LogUtils.info(LogUtils.FontColor.GREEN, LOGGER, "Updated {} successfully", backendLib);
        }
    }
}
