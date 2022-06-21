package io.github.nickid2018.koishibot.message.api;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.util.Either;

public interface ServiceMessage {

    ServiceMessage fillService(String name, Either<JsonObject, String> data);

    String getName();

    Either<JsonObject, String> getData();
}
