package io.github.nickid2018.koishibot.module.mc;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.module.KoishiBotModule;
import io.github.nickid2018.koishibot.module.mc.chat.MCVersionChecker;
import io.github.nickid2018.koishibot.util.GroupDataReader;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class MCModule extends KoishiBotModule {

    public static MCModule INSTANCE;
    public GroupDataReader<Set<String>> subscribedGroups;
    private MCVersionChecker versionChecker;

    public MCModule() {
        super("mc", List.of(
                new MCServerResolver(), new MCSkinResolver(), new MCSubscribeResolver()
        ), true);
        INSTANCE = this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onStartInternal() throws Exception {
        subscribedGroups = new GroupDataReader<>("mc",
                reader -> (Set<String>) new ObjectInputStream(reader).readObject(),
                (writer, data) -> new ObjectOutputStream(writer).writeObject(data),
                HashSet::new);
        subscribedGroups.loadAll();
        versionChecker = new MCVersionChecker(this);
        versionChecker.start();
    }

    @Override
    public void onSettingReloadInternal(JsonObject settingRoot) throws Exception {
    }

    @Override
    public void onTerminateInternal() throws Exception {
        versionChecker.stop();
    }

    @Override
    public String getDescription() {
        return "Minecraft 相关模块";
    }

    @Override
    public String getSummary() {
        return "链接 Minecraft 信息";
    }
}
