package io.github.nickid2018.koishibot.permission;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.core.ErrorRecord;
import io.github.nickid2018.koishibot.util.DataReader;
import io.github.nickid2018.koishibot.util.JsonUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionManager {

    private static final DataReader<Set<UserPermissionEntry>> DATA_READER = new DataReader<>(new File("permission.dat"), HashSet::new);
    private static final Map<String, UserPermissionEntry> PERMISSION_ENTRY_MAP = new ConcurrentHashMap<>();

    public static void init(JsonObject settingsRoot) throws IOException {
        JsonUtil.getData(settingsRoot, "owner", JsonArray.class).ifPresent(array -> array.forEach(element -> {
            if (element.isJsonPrimitive())
                PERMISSION_ENTRY_MAP.put(element.getAsString(),
                        UserPermissionEntry.permanentPermission(element.getAsString(), PermissionLevel.OWNER));
        }));
        DATA_READER.getData().forEach(entry -> PERMISSION_ENTRY_MAP.put(entry.user(), entry));
    }

    public static UserPermissionEntry getPermissionEntry(String user) {
        updateLevel(user);
        return PERMISSION_ENTRY_MAP.getOrDefault(user, UserPermissionEntry.permanentPermission(user, PermissionLevel.TRUSTED));
    }

    public static PermissionLevel getLevel(String user) {
        return getPermissionEntry(user).level();
    }

    private static void updateLevel(String user) {
        try {
            if (PERMISSION_ENTRY_MAP.containsKey(user) && PERMISSION_ENTRY_MAP.get(user).isExpired()) {
                UserPermissionEntry entry = PERMISSION_ENTRY_MAP.remove(user);
                DATA_READER.getData().remove(entry);
                DATA_READER.saveData();
            }
        } catch (IOException e) {
            ErrorRecord.enqueueError("permission.update", e);
        }
    }

    public static void setLevel(String target, PermissionLevel level, long expired, boolean userOperation) {
        try {
            UserPermissionEntry entry = PERMISSION_ENTRY_MAP.remove(target);
            if (entry != null && !userOperation &&
                    ((entry.level() == level && entry.expired() >= expired) || level.levelGreater(entry.level()))) {
                PERMISSION_ENTRY_MAP.put(target, entry);
                return;
            }
            if (entry != null) {
                DATA_READER.getData().remove(entry);
                DATA_READER.saveData();
            }
            if (level == PermissionLevel.TRUSTED)
                return;
            entry = new UserPermissionEntry(target, level, expired);
            PERMISSION_ENTRY_MAP.put(target, entry);
            DATA_READER.getData().add(entry);
            DATA_READER.saveData();
        } catch (IOException e) {
            ErrorRecord.enqueueError("permission.set", e);
        }
    }

    public static boolean tryGrantPermission(String operator, String target, PermissionLevel level, long expired) {
        if (getLevel(operator).levelGreater(PermissionLevel.TRUSTED) && getLevel(operator).levelGreater(getLevel(target))) {
            setLevel(target, level, expired, true);
            return true;
        } else return false;
    }
}
