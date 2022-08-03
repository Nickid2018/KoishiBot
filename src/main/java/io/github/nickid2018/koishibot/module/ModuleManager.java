package io.github.nickid2018.koishibot.module;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.message.api.ContactInfo;
import io.github.nickid2018.koishibot.message.api.GroupInfo;
import io.github.nickid2018.koishibot.module.github.GitHubModule;
import io.github.nickid2018.koishibot.module.mc.MCServerResolver;
import io.github.nickid2018.koishibot.module.mc.chat.MCChatBridgeModule;
import io.github.nickid2018.koishibot.module.translation.TranslateModule;
import io.github.nickid2018.koishibot.module.wiki.WikiModule;
import io.github.nickid2018.koishibot.resolver.*;
import io.github.nickid2018.koishibot.util.GroupDataReader;
import io.github.nickid2018.koishibot.util.value.MutableBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;

import static io.github.nickid2018.koishibot.module.SingleResolverModule.NOP;

public class ModuleManager {

    public static final Logger LOGGER = LoggerFactory.getLogger("Module Manager");

    private static final Map<String, Module> MODULE_MAP = new HashMap<>();

    @SuppressWarnings("unchecked")
    private static final GroupDataReader<Set<String>> DATA_READER = new GroupDataReader<>(
            "module",
            reader -> (Set<String>) new ObjectInputStream(reader).readObject(),
            (writer, data) -> new ObjectOutputStream(writer).writeObject(data),
            () -> new HashSet<>(MODULE_MAP.keySet())
    );

    // Module List
    static {
        addModule(new SingleResolverModule("help", true, NOP, NOP, new HelpResolver()));
        addModule(new SingleResolverModule("info", true, NOP, NOP, new InfoResolver()));
        addModule(new WikiModule());
        addModule(new SingleResolverModule("bilibili", true, NOP, NOP, new BilibiliDataResolver()));
        addModule(new TranslateModule());
        addModule(new SingleResolverModule("urban", true, NOP, NOP, new UrbanDictResolver()));
        addModule(new SingleResolverModule("latex", true, NOP, NOP, new LaTeXResolver()));
        addModule(new SingleResolverModule("qrcode", true, NOP, NOP, new QRCodeResolver()));
        addModule(new SingleResolverModule("bugtracker", true, NOP, NOP, new BugTrackerResolver()));
        addModule(new SingleResolverModule("mod", true, NOP, NOP, new CurseForgeResolver(), new ModrinthResolver()));
        addModule(new SingleResolverModule("mc-server", true, NOP, NOP, new MCServerResolver()));
        addModule(new MCChatBridgeModule());
        addModule(new GitHubModule());
        addModule(new SingleResolverModule("perm", false, NOP, NOP, new PermissionResolver()));
    }

    public static void start() {
        MODULE_MAP.forEach((name, module) -> {
            try {
                module.onStart();
                LOGGER.info("Started {}.", name);
            } catch (Exception e) {
                LOGGER.error("Starting failure: " + name, e);
            }
        });
    }

    public static void settingLoad(JsonObject setting) {
        MODULE_MAP.forEach((name, module) -> {
            try {
                module.onSettingReload(setting);
                LOGGER.info("Set {}.", name);
            } catch (Exception e) {
                LOGGER.error("Setting failure: " + name, e);
            }
        });
    }

    public static void stop() {
        MODULE_MAP.forEach((name, module) -> {
            try {
                module.onTerminate();
                LOGGER.info("Terminated {}.", name);
            } catch (Exception e) {
                LOGGER.error("Terminate failure: " + name, e);
            }
        });
    }

    public static void addModule(Module module) {
        MODULE_MAP.put(module.getName(), module);
    }

    public static boolean isOpened(String group, String moduleName) {
        return DATA_READER.getData(group).contains(moduleName);
    }

    public static boolean open(GroupInfo group, String moduleName) throws Exception {
        MutableBoolean success = new MutableBoolean(false);
        DATA_READER.updateData(group.getGroupId(), set -> {
            Module module = MODULE_MAP.get(moduleName);
            if (module == null)
                return set;
            set.add(moduleName);
            success.setValue(true);
            return set;
        });
        return success.getValue();
    }

    public static boolean close(GroupInfo group, String moduleName) throws Exception {
        MutableBoolean success = new MutableBoolean(false);
        DATA_READER.updateData(group.getGroupId(), set -> {
            Module module = MODULE_MAP.get(moduleName);
            if (module == null)
                return set;
            if (!module.isClosable())
                return set;
            set.remove(moduleName);
            success.setValue(true);
            return set;
        });
        return success.getValue();
    }

    public static List<MessageResolver> getAvailableResolvers(ContactInfo contact) {
        List<MessageResolver> available = new ArrayList<>();
        if (contact instanceof GroupInfo group)
            MODULE_MAP.keySet().stream()
                    .filter(val -> isOpened(group.getGroupId(), val))
                    .map(MODULE_MAP::get)
                    .forEach(m -> available.addAll(m.getResolvers()));
        else
            MODULE_MAP.values().forEach(m -> available.addAll(m.getResolvers()));
        return available;
    }
}
