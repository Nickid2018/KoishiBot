package io.github.nickid2018.koishibot.message;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.message.api.MessageContext;

public interface JSONServiceResolver {

    void resolveService(JsonObject content, MessageContext info, DelegateEnvironment environment);
}
