package io.github.nickid2018.koishibot.module;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.message.MessageResolver;

import java.util.List;

public class SingleResolverModule extends Module {

    public static final Runnable NOP = () -> {};

    private final String summary;
    private final Runnable start;
    private final Runnable terminate;

    public SingleResolverModule(String name, boolean closable, Runnable start, Runnable terminate, String summary,
                                MessageResolver... resolvers) {
        super(name, List.of(resolvers), closable);
        this.start = start;
        this.terminate = terminate;
        this.summary = summary;
    }

    @Override
    public void onStart() {
        start.run();
    }

    @Override
    public void onSettingReload(JsonObject settingRoot) {
    }

    @Override
    public void onTerminate() {
        terminate.run();
    }

    @Override
    public String getDescription() {
        return getSummary();
    }

    @Override
    public String getSummary() {
        return summary;
    }
}
