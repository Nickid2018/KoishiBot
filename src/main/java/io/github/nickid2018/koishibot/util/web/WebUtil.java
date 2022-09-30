package io.github.nickid2018.koishibot.util.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.github.nickid2018.koishibot.util.JsonUtil;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class WebUtil {

    public static final Logger WEB_LOGGER = LoggerFactory.getLogger("Web");

    public static final Set<String> SUPPORTED_IMAGE = Set.of(
            "jpg", "jpeg", "png", "bmp", "gif"
    );

    public static final String[] VIEWER_USER_AGENTS = new String[] {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:99.0) Gecko/20100101 Firefox/99.0",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.110 Safari/537.36 Edg/96.0.1054.62"
    };
    public static final Map<String, String> MIRROR = new HashMap<>();
    private static final Random UA_RANDOM = new Random();

    public static String chooseRandomUA() {
        return VIEWER_USER_AGENTS[UA_RANDOM.nextInt(VIEWER_USER_AGENTS.length)];
    }

    public static JsonElement fetchDataInJson(HttpUriRequest post) throws IOException {
        return fetchDataInJson(post, chooseRandomUA());
    }

    public static JsonElement fetchDataInJson(HttpUriRequest post, String UA) throws IOException {
        return fetchDataInJson(post, UA, true);
    }

    public static JsonElement fetchDataInJson(HttpUriRequest request, String UA, boolean check) throws IOException {
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .disableCookieManagement().useSystemProperties()
                .setUserAgent(UA).build();
        CloseableHttpResponse httpResponse = null;
        String json = null;
        try {
            httpResponse = httpClient.execute(request);
            int status = httpResponse.getCode();
            if (status / 100 != 2) {
                WEB_LOGGER.debug("Incorrect return code in requesting {}: {}", request.getRequestUri(), status);
                throw new ErrorCodeException(status);
            }
            if (check) {
                Header[] headers = httpResponse.getHeaders("Content-Type");
                if (headers.length > 0 && headers[0] != null && !headers[0].getValue()
                        .startsWith(ContentType.APPLICATION_JSON.getMimeType()))
                    throw new IOException("Return a non-JSON Content.");
            }
            HttpEntity httpEntity = httpResponse.getEntity();
            json = EntityUtils.toString(httpEntity, "UTF-8");
            EntityUtils.consume(httpEntity);
            return JsonParser.parseString(json);
        } catch (JsonSyntaxException jse) {
            if (json != null)
                WEB_LOGGER.debug("Incorrect JSON data in requesting {}: {}", request.getRequestUri(), json);
            throw jse;
        } catch (ParseException pe) {
            throw new IOException(pe);
        } finally {
            try {
                if (httpResponse != null)
                    httpResponse.close();
            } catch (IOException e) {
                WEB_LOGGER.error("## release resource error ##" + e);
            }
        }
    }

    public static void sendNeedCode(HttpUriRequest request, int code) throws IOException {
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .disableCookieManagement().useSystemProperties()
                .setUserAgent(chooseRandomUA()).build();
        CloseableHttpResponse httpResponse = null;
        try {
            httpResponse = httpClient.execute(request);
            int status = httpResponse.getCode();
            if (status != code) {
                WEB_LOGGER.debug("Incorrect return code in requesting {}: {}, required {}.",
                        request.getRequestUri(), status, code);
                throw new ErrorCodeException(status);
            }
        } finally {
            try {
                if (httpResponse != null)
                    httpResponse.close();
            } catch (IOException e) {
                WEB_LOGGER.error("## release resource error ##" + e);
            }
        }
    }

    public static void sendReturnNoContent(HttpUriRequest request) throws IOException {
        sendNeedCode(request, 204);
    }

    public static String fetchDataInText(HttpUriRequest post) throws IOException {
        return fetchDataInText(post, false);
    }

    public static String fetchDataInText(HttpUriRequest post, boolean ignoreErrorCode) throws IOException {
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .disableCookieManagement().useSystemProperties()
                .setUserAgent(chooseRandomUA()).build();
        CloseableHttpResponse httpResponse = null;
        try {
            httpResponse = httpClient.execute(post);
            int status = httpResponse.getCode();
            if (status / 100 != 2 && !ignoreErrorCode)
                throw new ErrorCodeException(status);
            HttpEntity httpEntity = httpResponse.getEntity();
            String text = EntityUtils.toString(httpEntity, "UTF-8");
            EntityUtils.consume(httpEntity);
            return text;
        } catch (ParseException pe) {
            throw new IOException(pe);
        } finally {
            try {
                if (httpResponse != null)
                    httpResponse.close();
            } catch (IOException e) {
                WEB_LOGGER.error("## release resource error ##" + e);
            }
        }
    }

    public static String getRedirected(HttpUriRequest request) throws IOException {
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .disableRedirectHandling().disableCookieManagement().useSystemProperties()
                .setUserAgent(chooseRandomUA()).build();
        CloseableHttpResponse httpResponse = null;
        try {
            httpResponse = httpClient.execute(request);
            if (httpResponse.getCode() != 302)
                return null;
            return httpResponse.getHeaders("location")[0].getValue();
        } finally {
            try {
                if (httpResponse != null)
                    httpResponse.close();
            } catch (IOException e) {
                WEB_LOGGER.error("## release resource error ##" + e);
            }
        }
    }

    public static String mirror(String url) {
        if (url.startsWith("//"))
            url = "https:" + url;
        for (Map.Entry<String, String> en : MIRROR.entrySet()) {
            String prefix = en.getKey();
            if (url.startsWith(prefix))
                return url.replace(prefix, en.getValue());
        }
        return url;
    }

    public static String encode(String str) {
        return URLEncoder.encode(str, StandardCharsets.UTF_8).replace(".", "%2E");
    }

    public static void loadMirror(JsonObject settingsRoot) {
        MIRROR.clear();
        JsonUtil.getData(settingsRoot, "mirrors", JsonObject.class).ifPresent(mirrors -> {
                    for (Map.Entry<String, JsonElement> en : mirrors.entrySet())
                        MIRROR.put(en.getKey(), en.getValue().getAsString());
                });
    }
}
