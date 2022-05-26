package io.github.nickid2018.koishibot.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.overzealous.remark.Options;
import com.overzealous.remark.Remark;
import com.overzealous.remark.convert.AbstractNodeHandler;
import com.overzealous.remark.convert.DocumentConverter;
import com.overzealous.remark.convert.NodeHandler;
import io.github.nickid2018.koishibot.KoishiBotMain;
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
import java.util.*;

public class WebUtil {

    public static final Set<String> SUPPORTED_IMAGE = new HashSet<>(
            Arrays.asList("jpg", "jpeg", "png", "bmp", "gif")
    );

    private static final Remark REMARK;
    private static final Logger LOGGER = LoggerFactory.getLogger("KoishiBot-Web");

    public static JsonElement fetchDataInJson(HttpUriRequest post) throws IOException {
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .disableCookieManagement().build();
        CloseableHttpResponse httpResponse = null;
        try {
            httpResponse = httpClient.execute(post);
            int status = httpResponse.getStatusLine().getStatusCode();
            if (status / 100 != 2)
                throw new ErrorCodeException(status);
            Header[] headers = httpResponse.getHeaders("Content-Type");
            if (headers.length > 0 && headers[0] != null && !headers[0].getValue()
                    .startsWith(ContentType.APPLICATION_JSON.getMimeType()))
                throw new IOException("Return a non-JSON Content.");
            HttpEntity httpEntity = httpResponse.getEntity();
            String json = EntityUtils.toString(httpEntity, "UTF-8");
            EntityUtils.consume(httpEntity);
            return JsonParser.parseString(json);
        } finally {
            try {
                if (httpResponse != null)
                    httpResponse.close();
            } catch (IOException e) {
                LOGGER.error("## release resource error ##" + e);
            }
        }
    }

    public static String fetchDataInPlain(HttpUriRequest post) throws IOException {
        return fetchDataInPlain(post, false);
    }

    public static String fetchDataInPlain(HttpUriRequest post, boolean ignoreErrorCode) throws IOException {
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .disableCookieManagement().build();
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
                LOGGER.error("## release resource error ##" + e);
            }
        }
    }

    public static String getRedirected(HttpUriRequest request) throws IOException {
        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .disableRedirectHandling().disableCookieManagement().build();
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
                LOGGER.error("## release resource error ##" + e);
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

    public static String getDataInPathOrNull(JsonObject root, String path) {
        String[] paths = path.split("\\.");
        JsonElement last = root;
        for (int i = 0; i < paths.length - 1; i++) {
            String key = paths[i];
            if (last instanceof JsonArray) {
                int num = Integer.parseInt(key);
                JsonArray array = (JsonArray) last;
                if (num >= array.size())
                    return null;
                last = array.get(num);
            } else if (last instanceof JsonObject) {
                JsonObject object = (JsonObject) last;
                if (!object.has(key))
                    return null;
                last = object.get(key);
            } else
                return null;
        }
        String name = paths[paths.length - 1];
        if (last instanceof JsonObject) {
            JsonObject now = (JsonObject) last;
            if (!now.has(name))
                return null;
            return now.get(name).getAsString();
        } else if (last instanceof JsonArray){
            JsonArray now = (JsonArray) last;
            int num = Integer.parseInt(name);
            if (num >= now.size())
                return null;
            return now.get(num).getAsString();
        } else
            return null;
    }

    public static String encode(String str) throws UnsupportedEncodingException {
        return URLEncoder.encode(str, "UTF-8").replace(".", "%2E");
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
