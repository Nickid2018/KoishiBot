package io.github.nickid2018.koishibot.module.wiki;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.module.Module;

import java.util.List;

public class WikiModule extends Module {

    public WikiModule() {
        super("wiki", List.of(
                new WikiResolver()
        ), true);
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onSettingReload(JsonObject settingRoot) {
        WikiInfo.loadWiki(settingRoot);
        InfoBoxShooter.loadWebDriver(settingRoot);
    }

    @Override
    public void onTerminate() {
        InfoBoxShooter.close();
    }
}
