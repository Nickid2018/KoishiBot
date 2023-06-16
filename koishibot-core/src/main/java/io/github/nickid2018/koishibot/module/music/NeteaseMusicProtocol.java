package io.github.nickid2018.koishibot.module.music;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.util.JsonUtil;
import io.github.nickid2018.koishibot.util.web.WebUtil;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.utils.Hex;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class NeteaseMusicProtocol {

    public static final BigInteger PUBLIC_KEY = new BigInteger("010001", 16);
    public static final BigInteger MODULUS = new BigInteger("00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b725152b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280104e0312ecbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932575cce10b424d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7", 16);

    public static JsonObject searchMusic(String keywords) throws IOException {
        HttpPost post = new HttpPost("http://music.163.com/api/search/get/");

        post.setHeader("Accept", "*/*");
        post.setHeader("Referer", "http://music.163.com");
        post.setHeader("Host", "music.163.com");

        List<NameValuePair> pairs = new ArrayList<>();
        pairs.add(new BasicNameValuePair("s", keywords));
        pairs.add(new BasicNameValuePair("type", "1"));
        pairs.add(new BasicNameValuePair("offset", "0"));
        pairs.add(new BasicNameValuePair("limit", "20"));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(pairs, StandardCharsets.UTF_8);
        post.setEntity(entity);

        return WebUtil.fetchDataInJson(post, WebUtil.chooseRandomUA(), false)
                .getAsJsonObject().getAsJsonObject("result");
    }

    public static Optional<JsonObject> getMusicInfo(int id) throws Exception {
        HttpPost post = new HttpPost("https://music.163.com/weapi/v3/song/detail");

        post.setHeader("Accept", "*/*");
        post.setHeader("Referer", "http://music.163.com");
        post.setHeader("Host", "music.163.com");

        JsonObject object = new JsonObject();
        object.addProperty("ids", "[%d]".formatted(id));
        object.addProperty("c", "[{\"id\":%d}]".formatted(id));
        object.addProperty("csrf_token", "");
        String data = object.toString();

        String[] encoded = generateParams(data);
        List<NameValuePair> pairs = new ArrayList<>();
        pairs.add(new BasicNameValuePair("params", encoded[0]));
        pairs.add(new BasicNameValuePair("encSecKey", encoded[1]));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(pairs, StandardCharsets.UTF_8);
        post.setEntity(entity);

        return JsonUtil.getDataInPath(
                WebUtil.fetchDataInJson(post, WebUtil.chooseRandomUA(), false).getAsJsonObject(),
                "songs.0", JsonObject.class);
    }

    public static String getMusicDataURL(int id) throws Exception {
        HttpPost post = new HttpPost("https://music.163.com/weapi/song/enhance/player/url");

        post.setHeader("Accept", "*/*");
        post.setHeader("Referer", "http://music.163.com");
        post.setHeader("Host", "music.163.com");

        JsonObject object = new JsonObject();
        JsonArray ids = new JsonArray();
        ids.add(id);
        object.add("ids", ids);
        object.addProperty("br", 240000);
        object.addProperty("csrf_token", "");
        String data = object.toString();

        String[] encoded = generateParams(data);
        List<NameValuePair> pairs = new ArrayList<>();
        pairs.add(new BasicNameValuePair("params", encoded[0]));
        pairs.add(new BasicNameValuePair("encSecKey", encoded[1]));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(pairs, StandardCharsets.UTF_8);
        post.setEntity(entity);

        return JsonUtil.getStringInPathOrNull(
                WebUtil.fetchDataInJson(post, WebUtil.chooseRandomUA(), false).getAsJsonObject(), "data.0.url");
    }

    public static String[] generateParams(String data) throws Exception {
        String secKey = generateSecretKey();
        String params = aesEncrypt(aesEncrypt(data, "0CoJUm6Qyw8W8jud"), secKey);
        String secKeyEncoded = rsaEncrypt(secKey);
        return new String[]{params, secKeyEncoded};
    }

    public static String generateSecretKey() {
        String randomChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 16; i++) {
            sb.append(randomChars.charAt(random.nextInt(randomChars.length())));
        }
        return sb.toString();
    }

    private static String aesEncrypt(String text, String key) throws Exception {
        int padding = 16 - text.length() % 16;
        text += String.valueOf((char) padding).repeat(padding);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        IvParameterSpec ivParameterSpec = new IvParameterSpec("0102030405060708".getBytes(StandardCharsets.UTF_8));
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
        return Base64.getUrlEncoder().encodeToString(cipher.doFinal(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static String rsaEncrypt(String text) {
        String reversed = new StringBuilder(text).reverse().toString();
        String hex = Hex.encodeHexString(reversed.getBytes(StandardCharsets.UTF_8));
        BigInteger integer = new BigInteger(hex, 16);
        BigInteger data = integer.modPow(PUBLIC_KEY, MODULUS);
        String over = data.toString(16);
        over = "0".repeat(256 - over.length()) + over;
        return over;
    }
}
