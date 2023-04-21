package io.github.nickid2018.koishibot.filter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.util.value.MutableBoolean;
import io.github.nickid2018.koishibot.util.web.WebUtil;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class BaiduSensitiveFilter extends SensitiveFilter {

    private final String apiKey;
    private final String apiSecret;

    private String accessToken;
    private long expireTime;

    public BaiduSensitiveFilter(String apiKey, String apiSecret) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        getAccessToken();
    }

    @Override
    protected String filter(String text, MutableBoolean filtered) {
        if (System.currentTimeMillis() > expireTime)
            getAccessToken();
        if (accessToken == null)
            return text;

        HttpPost post = new HttpPost("https://aip.baidubce.com/rest/2.0/solution/v1/text_censor/v2/user_defined?access_token=" + accessToken);
        post.setHeader("Accept", "application/json");
        StringEntity entity = new StringEntity("text=" + text, StandardCharsets.UTF_8);
        post.setEntity(entity);

        try {
            JsonObject object = WebUtil.fetchDataInJson(post).getAsJsonObject();
            if (object.has("error_code")) {
                SENSITIVE_LOGGER.error("Baidu Sensitive Filter Error: " + object.get("error_msg").getAsString());
                return text;
            }
            if (object.get("conclusionType").getAsInt() == 1)
                return text;
            filtered.setValue(true);
            for (JsonElement element : object.get("data").getAsJsonArray()) {
                JsonObject obj = element.getAsJsonObject();
                JsonArray hits = obj.getAsJsonArray("hits");
                if (hits.size() == 0)
                    continue;
                for (JsonElement hit : hits) {
                    JsonObject hitObj = hit.getAsJsonObject();
                    JsonArray words = hitObj.getAsJsonArray("words");
                    if (words.size() == 0)
                        continue;
                    JsonArray wordHitPositions = hitObj.getAsJsonArray("wordHitPositions");
                    if (wordHitPositions.size() == 0)
                        continue;
                    for (JsonElement wordHit : wordHitPositions) {
                        JsonObject wordHitObj = wordHit.getAsJsonObject();
                        String keyword = wordHitObj.get("keyword").getAsString();
                        JsonArray positions = wordHitObj.getAsJsonArray("positions");
                        if (positions.size() == 0)
                            text = "*filtered*" + text.substring(keyword.length());
                        else {
                            for (JsonElement position : positions) {
                                JsonArray positionObj = position.getAsJsonArray();
                                int start = positionObj.get(0).getAsInt();
                                int end = positionObj.get(1).getAsInt();
                                text = text.substring(0, start) + "*filtered*"+ text.substring(end + 1);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            return text;
        }

        return text;
    }

    private void getAccessToken() {
        HttpPost post = new HttpPost("https://aip.baidubce.com/oauth/2.0/token");
        post.setHeader("Accept", "application/json");

        List<NameValuePair> pairs = new ArrayList<>();
        pairs.add(new BasicNameValuePair("client_id", apiKey));
        pairs.add(new BasicNameValuePair("client_secret", apiSecret));
        pairs.add(new BasicNameValuePair("grant_type", "client_credentials"));

        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(pairs, StandardCharsets.UTF_8);
        post.setEntity(entity);

        try {
            JsonObject object = WebUtil.fetchDataInJson(post).getAsJsonObject();
            if (object.has("error")) {
                SENSITIVE_LOGGER.error("Failed to get access token: {}", object.get("error_description").getAsString());
                accessToken = null;
                expireTime = System.currentTimeMillis() + 60_000;
                return;
            }
            accessToken = object.get("access_token").getAsString();
            expireTime = System.currentTimeMillis() + object.get("expires_in").getAsLong() * 1000;
        } catch (IOException e) {
            accessToken = null;
            expireTime = System.currentTimeMillis() + 60_000;
            SENSITIVE_LOGGER.error("Failed to get access token", e);
        }
    }
}
