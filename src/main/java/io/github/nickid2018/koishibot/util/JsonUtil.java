package io.github.nickid2018.koishibot.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Optional;

public class JsonUtil {

    @SuppressWarnings("unchecked")
    public static <T extends JsonElement> Optional<T> getDataInPath(JsonObject root, String path, Class<T> type) {
        String[] paths = path.split("\\.");
        JsonElement last = root;
        for (int i = 0; i < paths.length - 1; i++) {
            String key = paths[i];
            if (last instanceof JsonArray) {
                int num = Integer.parseInt(key);
                JsonArray array = (JsonArray) last;
                if (num >= array.size())
                    return Optional.empty();
                last = array.get(num);
            } else if (last instanceof JsonObject) {
                JsonObject object = (JsonObject) last;
                if (!object.has(key))
                    return Optional.empty();
                last = object.get(key);
            } else
                return Optional.empty();
        }
        String name = paths[paths.length - 1];
        JsonElement get = null;
        if (last instanceof JsonObject) {
            JsonObject now = (JsonObject) last;
            if (!now.has(name))
                return Optional.empty();
            get = now.get(name);
        } else if (last instanceof JsonArray){
            JsonArray now = (JsonArray) last;
            int num = Integer.parseInt(name);
            if (num >= now.size())
                return Optional.empty();
            get = now.get(num);
        }
        return type.isInstance(get) ? Optional.of((T) get) : Optional.empty();
    }

    public static String getStringInPathOrNull(JsonObject root, String path) {
        return getDataInPath(root, path, JsonPrimitive.class)
                .map(JsonPrimitive::getAsString).orElse(null);
    }

    public static String getStringInPathOrElse(JsonObject root, String path, String el) {
        return getDataInPath(root, path, JsonPrimitive.class)
                .map(JsonPrimitive::getAsString).orElse(el);
    }
}
