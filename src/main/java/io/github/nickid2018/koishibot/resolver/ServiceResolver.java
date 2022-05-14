package io.github.nickid2018.koishibot.resolver;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.core.MessageInfo;

public interface ServiceResolver {

    void resolveService(JsonObject content, MessageInfo info);
}
