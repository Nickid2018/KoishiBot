package io.github.nickid2018.koishibot.monitor;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class WebRequests {

    private static final Logger WEB_LOGGER = LoggerFactory.getLogger("Web");

    public static void addGitHubHeaders(HttpUriRequest request) {
        if (Settings.GITHUB_TOKEN != null)
            request.addHeader("Authorization", "Bearer " + Settings.GITHUB_TOKEN);
        request.addHeader("Accept", "application/vnd.github.v3+json");
    }

    public static JsonElement fetchDataInJson(HttpUriRequest request) throws IOException {
        return fetchDataInJson(request, true);
    }

    public static JsonElement fetchDataInJson(HttpUriRequest request, boolean check) throws IOException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create()
                .disableCookieManagement()
                .useSystemProperties()
                .build()) {
            return httpClient.execute(request, response -> {
                int status = response.getCode();
                if (status / 100 != 2) {
                    WEB_LOGGER.debug("Incorrect return code in requesting {}: {}", request.getRequestUri(), status);
                    throw new IOException("Incorrect return code: " + status);
                }
                if (check) {
                    Header[] headers = response.getHeaders("Content-Type");
                    if (headers.length > 0 && headers[0] != null && !headers[0].getValue()
                            .startsWith(ContentType.APPLICATION_JSON.getMimeType()))
                        throw new IOException("Return a non-JSON Content.");
                }
                HttpEntity httpEntity = response.getEntity();
                String json = EntityUtils.toString(httpEntity, "UTF-8");
                try {
                    return JsonParser.parseString(json);
                } catch (JsonSyntaxException jse) {
                    if (json != null)
                        WEB_LOGGER.debug("Incorrect JSON data in requesting {}: {}", request.getRequestUri(), json);
                    throw jse;
                }
            });
        }
    }

    public static byte[] fetchDataInBytes(HttpUriRequest request) throws IOException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create()
                .disableCookieManagement()
                .useSystemProperties()
                .build()) {
            return httpClient.execute(request, response -> {
                int status = response.getCode();
                if (status / 100 != 2) {
                    WEB_LOGGER.debug("Incorrect return code in requesting {}: {}", request.getRequestUri(), status);
                    throw new IOException("Incorrect return code: " + status);
                }
                HttpEntity httpEntity = response.getEntity();
                return EntityUtils.toByteArray(httpEntity);
            });
        }
    }
}
