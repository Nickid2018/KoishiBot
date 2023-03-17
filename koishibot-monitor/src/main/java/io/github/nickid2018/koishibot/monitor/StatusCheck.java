package io.github.nickid2018.koishibot.monitor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;

import java.io.FileReader;
import java.io.IOException;

public class StatusCheck {

    public static JsonObject readNowStatus() throws IOException {
        return JsonParser.parseString(IOUtils.toString(new FileReader(CreateAStart.MONITOR_DATA_FILE))).getAsJsonObject();
    }
}
