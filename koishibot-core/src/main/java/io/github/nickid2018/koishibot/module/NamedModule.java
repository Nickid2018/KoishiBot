package io.github.nickid2018.koishibot.module;

import com.google.gson.JsonObject;

import java.util.List;

public class NamedModule extends KoishiBotModule {

    private final String desc;

    public NamedModule(String name, boolean closable, String desc) {
        super(name, List.of(), closable);
        this.desc = desc;
    }

    @Override
    public void onStartInternal() {
    }

    @Override
    public void onSettingReloadInternal(JsonObject settingRoot) {
    }

    @Override
    public void onTerminateInternal() {
    }

    @Override
    public String getDescription() {
        return desc;
    }

    @Override
    public String getSummary() {
        return desc;
    }
}
