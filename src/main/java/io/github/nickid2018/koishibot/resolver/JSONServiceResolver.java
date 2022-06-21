package io.github.nickid2018.koishibot.resolver;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;

public interface JSONServiceResolver {

    void resolveService(JsonObject content, MessageContext info, Environment environment);
}
