package io.github.nickid2018.koishibot.monitor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.nickid2018.koishibot.util.JsonUtil;
import io.github.nickid2018.koishibot.util.LogUtils;
import io.github.nickid2018.koishibot.util.Pair;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class StatusCheck {

    public static Pair<Set<String>, Set<String>> needUpdates(File checksumsFile) throws IOException {
        Set<String> allCores = new HashSet<>();
        Set<String> needUpdateCores = new HashSet<>();
        Set<String> allLibs = new HashSet<>();
        Set<String> needUpdateLibs = new HashSet<>();
        try (ZipFile zipFile = new ZipFile(checksumsFile)) {
            Objects.<Map<String, Pair<String, String>>>requireNonNullElse(readNowStatus(), new HashMap<>()).forEach((key, checksums) -> {
                allCores.add(key);
                allLibs.add(key);
                try {
                    Pair<String, String> checksumsNow = getChecksumNowAction(zipFile, key);
                    if (checksumsNow == null) {
                        LogUtils.error(MonitorStart.LOGGER, "Can't resolve key \"%s\"".formatted(key), null);
                        return;
                    }
                    if (checksumsNow.first() != null) {
                        allCores.add(key);
                        if (!checksumsNow.first().equals(checksums.first()))
                            needUpdateCores.add(key);
                    }
                    if (checksumsNow.second() != null) {
                        allLibs.add(key);
                        if (!checksumsNow.second().equals(checksums.second()))
                            needUpdateLibs.add(key);
                    }
                } catch (IOException e) {
                    LogUtils.error(MonitorStart.LOGGER, "Error when checking status of %s".formatted(key), e);
                }
            });
        }
        if (!allCores.contains("core"))
            needUpdateCores.add("core");
        if (!allLibs.contains("core"))
            needUpdateLibs.add("core");
        for (String backend : Settings.ENABLE_BACKENDS) {
            if (!allCores.contains(backend))
                needUpdateCores.add(backend);
            if (!allLibs.contains(backend))
                needUpdateLibs.add(backend);
        }
        return new Pair<>(needUpdateCores, needUpdateLibs);
    }

    public static void updateChecksums(File checksumsFile, Set<String> moduleNames) throws IOException {
        Map<String, Pair<String, String>> map = Objects.requireNonNullElse(readNowStatus(), new HashMap<>());
        try (ZipFile zipFile = new ZipFile(checksumsFile)) {
            for (String key : moduleNames) {
                Pair<String, String> checksumsNow = getChecksumNowAction(zipFile, key);
                if (checksumsNow == null) {
                    LogUtils.error(MonitorStart.LOGGER, "Can't resolve key \"%s\"".formatted(key), null);
                    continue;
                }
                map.put(key, checksumsNow);
            }
        }
        JsonObject json = new JsonObject();
        map.forEach((key, checksums) -> {
            JsonObject obj = new JsonObject();
            obj.addProperty("checksum_core", checksums.first());
            obj.addProperty("checksum_libraries", checksums.second());
            json.add(key, obj);
        });
        try (FileWriter fw = new FileWriter(EnvironmentCheck.MONITOR_DATA_FILE)) {
            fw.write(json.toString());
        }
    }

    public static Map<String, Pair<String, String>> readNowStatus() throws IOException {
        JsonObject json;
        try {
            json = JsonParser.parseString(IOUtils.toString(new FileReader(EnvironmentCheck.MONITOR_DATA_FILE))).getAsJsonObject();
        } catch (FileNotFoundException e) {
            return null;
        }
        Map<String, Pair<String, String>> map = new HashMap<>();
        for (String key : json.keySet()) {
            JsonObject obj = json.getAsJsonObject(key);
            String checksum = JsonUtil.getStringOrNull(obj, "checksum_core");
            String checksumLib = JsonUtil.getStringOrNull(obj, "checksum_libraries");
            map.put(key, new Pair<>(checksum, checksumLib));
        }
        return map;
    }

    public static Pair<String, String> getChecksumNowAction(ZipFile checksumZip, String key) throws IOException {
        ZipEntry entry = checksumZip.getEntry(key + "-checksum.txt");
        if (entry == null)
            return null;
        String[] args = IOUtils.toString(checksumZip.getInputStream(entry), StandardCharsets.UTF_8).split("\n");
        if (args.length != 2)
            return null;
        return new Pair<>(args[0], args[1]);
    }
}
