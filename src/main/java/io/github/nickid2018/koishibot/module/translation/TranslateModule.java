package io.github.nickid2018.koishibot.module.translation;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.module.Module;

import java.util.List;

public class TranslateModule extends Module {

    public TranslateModule() {
        super("translate", List.of(new TranslateResolver()), true);
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onSettingReload(JsonObject settingRoot) {
        YoudaoTranslation.loadYouDaoAppKeyAndSecrets(settingRoot);
    }

    @Override
    public void onTerminate() {
    }
}
