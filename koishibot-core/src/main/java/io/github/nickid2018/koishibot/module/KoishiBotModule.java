package io.github.nickid2018.koishibot.module;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.message.MessageResolver;

import java.util.Collections;
import java.util.List;

public abstract class KoishiBotModule {

    private final String name;
    private final boolean closable;
    private final List<MessageResolver> resolverList;

    private ModuleStatus status = ModuleStatus.READY;

    public KoishiBotModule(String name, List<MessageResolver> resolvers, boolean closable) {
        this.name = name;
        this.closable = closable;
        resolverList = Collections.unmodifiableList(resolvers);
    }

    public String getName() {
        return name;
    }

    public List<MessageResolver> getResolvers() {
        return resolverList;
    }

    public boolean isClosable() {
        return closable;
    }

    public void onStart() throws Exception {
        onStartInternal();
        status = ModuleStatus.INITIALIZED;
    }
    public void onSettingReload(JsonObject settingRoot) throws Exception {
        onSettingReloadInternal(settingRoot);
        status = status != ModuleStatus.INITIALIZED ? ModuleStatus.SET : ModuleStatus.INITIALIZED;
    }
    public void onTerminate() throws Exception {
        onTerminateInternal();
        status = ModuleStatus.TERMINATED;
    }

    public void setStatus(ModuleStatus status) {
        this.status = status;
    }

    public ModuleStatus getStatus() {
        return status;
    }

    public abstract void onStartInternal() throws Exception;
    public abstract void onSettingReloadInternal(JsonObject settingRoot) throws Exception;
    public abstract void onTerminateInternal() throws Exception;

    public abstract String getDescription();

    public abstract String getSummary();
}
