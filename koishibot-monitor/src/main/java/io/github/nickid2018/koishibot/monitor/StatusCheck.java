package io.github.nickid2018.koishibot.monitor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.nickid2018.koishibot.util.JsonUtil;
import io.github.nickid2018.koishibot.util.Pair;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class StatusCheck {

    public static Pair<Set<String>, Set<String>> needUpdates(File checksumsFile) throws IOException {
        Set<String> needUpdateCores = new HashSet<>();
        Set<String> needUpdateLibs = new HashSet<>();
        try (ZipFile zipFile = new ZipFile(checksumsFile)) {
            readNowStatus().forEach((key, checksums) -> {
                try {
                    Pair<String, String> checksumsNow = getChecksumNowAction(zipFile, key);
                    if (checksumsNow == null) {
                        MonitorStart.LOGGER.error("Can't resolve key \"%s\"".formatted(key));
                        return;
                    }
                    if (!checksumsNow.first().equals(checksums.first()))
                        needUpdateCores.add(key);
                    if (!checksumsNow.second().equals(checksums.second()))
                        needUpdateLibs.add(key);
                } catch (IOException e) {
                    MonitorStart.LOGGER.error("Error when checking status of " + key, e);
                }
            });
        }
        return new Pair<>(needUpdateCores, needUpdateLibs);
    }

    public static void updateChecksums(File checksumsFile, Set<String> moduleNames) throws IOException {
        Map<String, Pair<String, String>> map = new HashMap<>();
        try (ZipFile zipFile = new ZipFile(checksumsFile)) {
            for (String key : moduleNames) {
                Pair<String, String> checksumsNow = getChecksumNowAction(zipFile, key);
                if (checksumsNow == null) {
                    MonitorStart.LOGGER.error("Can't resolve key \"%s\"".formatted(key));
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
        try (FileWriter fw = new FileWriter(CreateAStart.MONITOR_DATA_FILE)) {
            fw.write(json.toString());
        }
    }

    public static Map<String, Pair<String, String>> readNowStatus() throws IOException {
        JsonObject json = JsonParser.parseString(IOUtils.toString(new FileReader(CreateAStart.MONITOR_DATA_FILE))).getAsJsonObject();
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
        ZipEntry entry = checksumZip.getEntry(key);
        if (entry == null)
            return null;
        String[] args = IOUtils.toString(checksumZip.getInputStream(entry), StandardCharsets.UTF_8).split("\n");
        if (args.length != 2)
            return null;
        return new Pair<>(args[0], args[1]);
    }
}
