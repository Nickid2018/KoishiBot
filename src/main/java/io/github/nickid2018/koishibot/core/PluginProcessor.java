package io.github.nickid2018.koishibot.core;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.filter.SensitiveWordFilter;
import io.github.nickid2018.koishibot.github.GitHubListener;
import io.github.nickid2018.koishibot.mc.chat.MCChatBridge;
import io.github.nickid2018.koishibot.message.MemberFilter;
import io.github.nickid2018.koishibot.server.ServerManager;
import io.github.nickid2018.koishibot.translation.YoudaoTranslation;
import io.github.nickid2018.koishibot.util.AsyncUtil;
import io.github.nickid2018.koishibot.util.ImageRenderer;
import io.github.nickid2018.koishibot.util.WebUtil;
import io.github.nickid2018.koishibot.wiki.FormatTransformer;
import io.github.nickid2018.koishibot.wiki.InfoBoxShooter;
import io.github.nickid2018.koishibot.wiki.WikiInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class PluginProcessor {

    private static final List<Method> INIT_PROCESS = new ArrayList<>();
    private static final List<Method> EXIT_PROCESS = new ArrayList<>();
    private static final List<Method> SETTING_LOAD = new ArrayList<>();

    public static final File PLUGIN_FILE = new File("pluginProcess.json");
    public static final Logger LOGGER = LoggerFactory.getLogger("Plugin Processor");

    public static void initProcess() {
        try {
            INIT_PROCESS.add(AsyncUtil.class.getMethod("start"));
            EXIT_PROCESS.add(TempFileSystem.class.getMethod("close"));
            EXIT_PROCESS.add(ServerManager.class.getMethod("stop"));
            EXIT_PROCESS.add(AsyncUtil.class.getMethod("terminate"));
            SETTING_LOAD.add(Settings.class.getMethod("loadProxy", JsonObject.class));
            SETTING_LOAD.add(WebUtil.class.getMethod("loadMirror", JsonObject.class));
        } catch (NoSuchMethodException ignored) {
        }
        if (!PLUGIN_FILE.exists()) {
            LOGGER.info("No plugin description data found. Using default config.");
            try {
                INIT_PROCESS.add(MemberFilter.class.getMethod("init"));
                INIT_PROCESS.add(MCChatBridge.class.getMethod("init"));
                EXIT_PROCESS.add(0, InfoBoxShooter.class.getMethod("close"));
                SETTING_LOAD.add(SensitiveWordFilter.class.getMethod("loadSensitiveWordsSettings", JsonObject.class));
                SETTING_LOAD.add(WikiInfo.class.getMethod("loadWiki", JsonObject.class));
                SETTING_LOAD.add(ImageRenderer.class.getMethod("loadImageSettings", JsonObject.class));
                SETTING_LOAD.add(FormatTransformer.class.getMethod("loadFFmpeg", JsonObject.class));
                SETTING_LOAD.add(InfoBoxShooter.class.getMethod("loadWebDriver", JsonObject.class));
                SETTING_LOAD.add(GitHubListener.class.getMethod("loadGitHub", JsonObject.class));
                SETTING_LOAD.add(YoudaoTranslation.class.getMethod("loadYouDaoAppKeyAndSecrets", JsonObject.class));
            } catch (NoSuchMethodException ignored) {
            }
        }
    }

    public static void init() {
        INIT_PROCESS.forEach(method -> {
            try {
                method.invoke(null);
            } catch (Throwable e) {
                LOGGER.error("Initializing process error.", e);
            }
        });
    }

    public static void exit() {
        EXIT_PROCESS.forEach(method -> {
            try {
                method.invoke(null);
            } catch (Exception e) {
                LOGGER.error("Exiting process error.", e);
            }
        });
    }

    public static void set(JsonObject settingRoot) {
        SETTING_LOAD.forEach(method -> {
            try {
                method.invoke(null, settingRoot);
            } catch (Exception e) {
                LOGGER.error("Setting process error.", e);
            }
        });
    }
}
