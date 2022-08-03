package io.github.nickid2018.koishibot.module;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.resolver.MessageResolver;

import java.util.Collections;
import java.util.List;

public abstract class Module {

    private final String name;
    private final boolean closable;
    private final List<MessageResolver> resolverList;

    public Module(String name, List<MessageResolver> resolvers, boolean closable) {
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


    public abstract void onStart() throws Exception;
    public abstract void onSettingReload(JsonObject settingRoot) throws Exception;
    public abstract void onTerminate() throws Exception;
}
