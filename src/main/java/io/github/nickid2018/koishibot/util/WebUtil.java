package io.github.nickid2018.koishibot.util;

import com.google.gson.*;
import com.overzealous.remark.Options;
import com.overzealous.remark.Remark;
import com.overzealous.remark.convert.AbstractNodeHandler;
import com.overzealous.remark.convert.DocumentConverter;
import com.overzealous.remark.convert.NodeHandler;
import io.github.nickid2018.koishibot.core.Settings;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class WebUtil {

    public static final Logger WEB_LOGGER = LoggerFactory.getLogger("Web");

    public static final Set<String> SUPPORTED_IMAGE = new HashSet<>(
            Arrays.asList("jpg", "jpeg", "png", "bmp", "gif")
    );

    public static final String[] VIEWER_USER_AGENTS = new String[] {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:99.0) Gecko/20100101 Firefox/99.0",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.110 Safari/537.36 Edg/96.0.1054.62"
    };

    private static final Remark REMARK;
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
                .disableCookieManagement()
                .setUserAgent(UA).build();
        CloseableHttpResponse httpResponse = null;
        String json = null;
        try {
            httpResponse = httpClient.execute(request);
            int status = httpResponse.getStatusLine().getStatusCode();
            if (status / 100 != 2) {
                WEB_LOGGER.debug("Incorrect return code in requesting {}: {}", request.getURI(), status);
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
                WEB_LOGGER.debug("Incorrect JSON data in requesting {}: {}", request.getURI(), json);
            throw jse;
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
                .disableCookieManagement()
                .setUserAgent(chooseRandomUA()).build();
        CloseableHttpResponse httpResponse = null;
        try {
            httpResponse = httpClient.execute(request);
            int status = httpResponse.getStatusLine().getStatusCode();
            if (status != code) {
                WEB_LOGGER.debug("Incorrect return code in requesting {}: {}, required {}.",
                        request.getURI(), status, code);
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
                .disableCookieManagement()
                .setUserAgent(chooseRandomUA()).build();
        CloseableHttpResponse httpResponse = null;
        try {
            httpResponse = httpClient.execute(post);
            int status = httpResponse.getStatusLine().getStatusCode();
            if (status / 100 != 2 && !ignoreErrorCode)
                throw new ErrorCodeException(status);
            HttpEntity httpEntity = httpResponse.getEntity();
            String text = EntityUtils.toString(httpEntity, "UTF-8");
            EntityUtils.consume(httpEntity);
            return text;
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
                .disableRedirectHandling().disableCookieManagement()
                .setUserAgent(chooseRandomUA()).build();
        CloseableHttpResponse httpResponse = null;
        try {
            httpResponse = httpClient.execute(request);
            if (httpResponse.getStatusLine().getStatusCode() != 302)
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
        for (Map.Entry<String, String> en : Settings.MIRROR.entrySet()) {
            String prefix = en.getKey();
            if (url.startsWith(prefix))
                return url.replace(prefix, en.getValue());
        }
        return url;
    }

    public static String encode(String str) throws UnsupportedEncodingException {
        return URLEncoder.encode(str, StandardCharsets.UTF_8).replace(".", "%2E");
    }

    public static String getAsMarkdownClean(String str) {
        StringBuilder detail = new StringBuilder();
        new BufferedReader(new StringReader(REMARK.convert(str))).lines().forEach(s -> {
            s = s.trim();
            if (!s.isEmpty())
                detail.append(s.replaceAll("\\\\\\[\\d*\\\\]", "")).append("\n");
        });
        return detail.toString();
    }

    static {
        Options options = new Options();
        options.tables = Options.Tables.REMOVE;
        options.hardwraps = true;
        REMARK = new Remark(options);
        DocumentConverter converter = REMARK.getConverter();
        converter.addInlineNode(new AbstractNodeHandler() {
            @Override
            public void handleNode(NodeHandler nodeHandler, Element element, DocumentConverter documentConverter) {
            }
        }, "img");
        converter.addInlineNode(new AbstractNodeHandler() {
            @Override
            public void handleNode(NodeHandler parent, Element node, DocumentConverter documentConverter) {
                converter.walkNodes(parent, node);
            }
        }, "a,i,em,b,strong,font,span,code");
    }
}
