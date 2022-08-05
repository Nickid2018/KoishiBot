package io.github.nickid2018.koishibot.module.translation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.util.JsonUtil;
import io.github.nickid2018.koishibot.util.ReflectTarget;
import io.github.nickid2018.koishibot.util.web.WebUtil;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YoudaoTranslation implements TranslationProvider {

    private static final String YOUDAO_URL = "https://openapi.youdao.com/api";
    private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static String YOUDAO_APP_KEY;
    public static String YOUDAO_APP_SECRET;

    @ReflectTarget
    public static void loadYouDaoAppKeyAndSecrets(JsonObject settingsRoot) {
        JsonUtil.getData(settingsRoot, "youdao", JsonObject.class).ifPresent(youdao -> {
            YOUDAO_APP_KEY = JsonUtil.getStringOrNull(youdao, "app_key");
            YOUDAO_APP_SECRET = JsonUtil.getStringOrNull(youdao, "app_secret");
        });
    }

    @Override
    public String translate(String text, String from, String to) throws IOException {
        // Parameters
        Map<String, String> params = new HashMap<>();
        String salt = String.valueOf(System.currentTimeMillis());
        if (from != null && !from.isEmpty())
            params.put("from", from);
        if (to != null && !to.isEmpty())
            params.put("to", to);
        params.put("signType", "v3");
        String curtime = String.valueOf(System.currentTimeMillis() / 1000);
        params.put("curtime", curtime);
        String signStr = YOUDAO_APP_KEY + truncate(text) + salt + curtime + YOUDAO_APP_SECRET;
        String sign = getDigest(signStr);
        params.put("appKey", YOUDAO_APP_KEY);
        params.put("q", text);
        params.put("salt", salt);
        params.put("sign", sign);
        // Request
        HttpPost httpPost = new HttpPost(YOUDAO_URL);
        List<NameValuePair> paramsList = new ArrayList<>();
        for (Map.Entry<String, String> en : params.entrySet()) {
            String key = en.getKey();
            String value = en.getValue();
            paramsList.add(new BasicNameValuePair(key, value));
        }
        httpPost.setEntity(new UrlEncodedFormEntity(paramsList, StandardCharsets.UTF_8));

        JsonObject object = WebUtil.fetchDataInJson(httpPost).getAsJsonObject();
        int errorCode = object.getAsJsonPrimitive("errorCode").getAsInt();
        if (errorCode != 0)
            throw new IOException("API接口返回错误码" + errorCode);
        if (object.has("basic")) {
            // SingleChar Mode
            StringBuilder sb = new StringBuilder();
            JsonObject word = object.getAsJsonObject("basic");
            word.getAsJsonArray("explains").forEach(str -> sb.append(str.getAsString()).append("\n"));
            return sb.toString().trim();
        } else {
            JsonArray translation = object.getAsJsonArray("translation");
            StringBuilder sb = new StringBuilder();
            translation.forEach(str -> sb.append(str.getAsString()).append("\n"));
            return sb.toString().trim();
        }
    }

    public static String getDigest(String string) {
        if (string == null)
            return null;
        byte[] btInput = string.getBytes(StandardCharsets.UTF_8);
        try {
            MessageDigest mdInst = MessageDigest.getInstance("SHA-256");
            mdInst.update(btInput);
            byte[] md = mdInst.digest();
            int j = md.length;
            char[] str = new char[j * 2];
            int k = 0;
            for (byte byte0 : md) {
                str[k++] = HEX_DIGITS[byte0 >>> 4 & 0xf];
                str[k++] = HEX_DIGITS[byte0 & 0xf];
            }
            return new String(str);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    public static String truncate(String q) {
        if (q == null)
            return null;
        int len = q.length();
        return len <= 20 ? q : (q.substring(0, 10) + len + q.substring(len - 10, len));
    }
}
