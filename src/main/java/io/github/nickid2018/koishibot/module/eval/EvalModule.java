package io.github.nickid2018.koishibot.module.eval;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.module.Module;

import java.util.List;

public class EvalModule extends Module {

    public EvalModule() {
        super("eval", List.of(), true);
    }

    @Override
    public void onStartInternal() throws Exception {

    }

    @Override
    public void onSettingReloadInternal(JsonObject settingRoot) throws Exception {

    }

    @Override
    public void onTerminateInternal() throws Exception {

    }

    @Override
    public String getDescription() {
        return "函数模块";
    }

    @Override
    public String getSummary() {
        return "设置可配置的函数";
    }
}
