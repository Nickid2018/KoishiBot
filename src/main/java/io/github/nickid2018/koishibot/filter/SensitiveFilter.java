package io.github.nickid2018.koishibot.filter;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.message.api.*;
import io.github.nickid2018.koishibot.permission.PermissionLevel;
import io.github.nickid2018.koishibot.permission.PermissionManager;
import io.github.nickid2018.koishibot.util.JsonUtil;
import io.github.nickid2018.koishibot.util.value.MutableBoolean;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class SensitiveFilter implements PostFilter {

    public static SensitiveFilter INSTANCE;

    public static final Logger SENSITIVE_LOGGER = LoggerFactory.getLogger("Sensitive Filter");

    public static void loadSensitiveSettings(JsonObject settingsRoot) {
        JsonUtil.getData(settingsRoot, "sensitives", JsonObject.class).ifPresent(s -> {
            boolean baiduAPI = JsonUtil.getString(s, "type").map(l -> l.equalsIgnoreCase("baidu")).orElse(false);
            if (baiduAPI) {
                String appId = JsonUtil.getStringOrNull(s, "appId");
                String appKey = JsonUtil.getStringOrNull(s, "appKey");
                INSTANCE = new BaiduSensitiveFilter(appId, appKey);
                SENSITIVE_LOGGER.info("Sensitive filter is using Baidu API.");
            } else {
                try {
                    SensitiveWordFilter.loadWordFromFile(JsonUtil.getStringOrNull(s, "file"));
                } catch (IOException ignored) {
                }
                INSTANCE = new SensitiveWordFilter();
                SENSITIVE_LOGGER.info("Sensitive word filter initialized.");
            }
        });
    }

    @NotNull
    @Override
    public AbstractMessage filterMessagePost(AbstractMessage input, MessageContext context, Environment environment) {
        MutableBoolean filtered = new MutableBoolean(false);
        if (input instanceof ChainMessage) {
            List<AbstractMessage> messages = new ArrayList<>();
            for (AbstractMessage mess : ((ChainMessage) input).getMessages()) {
                if (mess instanceof TextMessage text)
                    mess = environment.newText(filter(text.getText(), filtered));
                messages.add(mess);
            }
            input = environment.newChain(messages.toArray(new AbstractMessage[0]));
        } else if (input instanceof TextMessage text)
            input = environment.newText(filter(text.getText(), filtered));
        if (filtered.getValue() && PermissionLevel.TRUSTED.levelGreaterOrEquals(PermissionManager.getLevel(context.user().getUserId())))
            PermissionManager.setLevel(context.user().getUserId(), PermissionLevel.UNTRUSTED,
                    System.currentTimeMillis() + 60_000, false);
        return input;
    }

    protected abstract String filter(String text, MutableBoolean filtered);
}
