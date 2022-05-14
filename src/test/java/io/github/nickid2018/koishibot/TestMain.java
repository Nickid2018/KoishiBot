package io.github.nickid2018.koishibot;

import com.google.gson.JsonParser;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class TestMain {

    public static void main(String[] args) throws IOException {
        CloseableHttpClient httpClient = HttpClientBuilder.create().disableRedirectHandling().build();
        CloseableHttpResponse httpResponse = null;
        try {
            HttpGet get = new HttpGet("https://b23.tv/QwEADkc");
            httpResponse = httpClient.execute(get);
            System.out.println(httpResponse.getStatusLine());
            System.out.println(httpResponse.getHeaders("location")[0].getValue());
        } finally {
            try {
                if (httpResponse != null)
                    httpResponse.close();
            } catch (IOException e) {
                KoishiBotMain.INSTANCE.getLogger().error("## release resource error ##" + e);
            }
        }
    }
}
