package io.github.nickid2018.koishibot.module.wiki;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.module.KoishiBotModule;

import java.util.List;

public class WikiModule extends KoishiBotModule {

    public WikiModule() {
        super("wiki", List.of(
                new WikiResolver(), new WikiSettingResolver()
        ), true);
    }

    @Override
    public void onStartInternal() {
    }

    @Override
    public void onSettingReloadInternal(JsonObject settingRoot) {
        WikiInfo.loadWiki(settingRoot);
    }

    @Override
    public void onTerminateInternal() {
    }

    @Override
    public String getDescription() {
        return "提供查询wiki的功能";
    }

    @Override
    public String getSummary() {
        return """
                查询wiki。
                可以调取wiki内的图片和音频，音频将以24000采样率转换为silk语音，分割为每段为110s的片段每2min发送一次。
                **不要频繁调用音频文件，这对于bot来说是很大的负担，并且发送过程中会执行阻塞，请保证上一段音频和下一段音频的请求时间大于1h。**
                """;
    }
}
