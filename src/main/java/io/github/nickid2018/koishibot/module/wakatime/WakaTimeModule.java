package io.github.nickid2018.koishibot.module.wakatime;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.module.Module;
import io.github.nickid2018.koishibot.util.JsonUtil;
import io.github.nickid2018.koishibot.util.web.OAuth2Authenticator;

import java.io.IOException;
import java.util.List;

public class WakaTimeModule extends Module {

    private OAuth2Authenticator authenticator;

    public static WakaTimeModule INSTANCE;

    public WakaTimeModule() {
        super("wakatime", List.of(new WakaTimeResolver()), true);
        INSTANCE = this;
    }

    public OAuth2Authenticator getAuthenticator() {
        return authenticator;
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onSettingReload(JsonObject settingRoot) {
        if (authenticator != null)
            authenticator.close();
        JsonUtil.getData(settingRoot, "wakatime", JsonObject.class).ifPresent(oauth -> {
            try {
                authenticator = new OAuth2Authenticator("wakatime", "https://wakatime.com/oauth/authorize",
                        "https://wakatime.com/oauth/token", "https://wakatime.com//oauth/revoke", true,
                        "/wakaTimeOAuth",
                        JsonUtil.getStringOrNull(oauth, "client_id"), JsonUtil.getStringOrNull(oauth, "client_secret"), true
                );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void onTerminate() {
        authenticator.close();
    }

    @Override
    public String getDescription() {
        return "WakaTime模块";
    }

    @Override
    public String getSummary() {
        return "使用WakaTime进行编程工作统计。";
    }
}
