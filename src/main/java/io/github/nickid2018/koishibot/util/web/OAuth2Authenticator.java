package io.github.nickid2018.koishibot.util.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.http.client.methods.HttpPost;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class OAuth2Authenticator implements HttpHandler {

    private final String authenticateURL;
    private final String tokenGrantURL;
    private final String redirect;
    private final String clientID;
    private final String clientSecret;

    private final Map<String, Consumer<String>> authSequence = new ConcurrentHashMap<>();

    public OAuth2Authenticator(String authenticateURL, String tokenGrantURL,
                               String redirect, String clientID, String clientSecret) {
        this.authenticateURL = authenticateURL;
        this.tokenGrantURL = tokenGrantURL;
        this.redirect = redirect;
        this.clientID = clientID;
        this.clientSecret = clientSecret;
    }

    public void authenticate(Consumer<String> authURLSender, Consumer<String> operation,
                             List<String> scopes,  Map<String, String> extraParameters) {
        String state = randomState();
        String url = "%s?client_id=%s&state=%s&scopes=%s&response_type=code".formatted(
                authenticateURL, clientID, state, String.join(",", scopes));
        if (redirect != null)
            url += "&" + redirect;

        String extra = extraParameters.entrySet().stream()
                .map(en -> WebUtil.encode(en.getKey()) + "=" + WebUtil.encode(en.getValue()))
                .collect(Collectors.joining("&"));

        if (!extra.isEmpty())
            url += "&" + extra;

        authSequence.put(state, operation);
        authURLSender.accept(url);
    }

    private static String randomState() {
        UUID uuid = UUID.randomUUID();
        return Long.toHexString(uuid.getMostSignificantBits()) + Long.toHexString(uuid.getLeastSignificantBits());
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        String query = httpExchange.getRequestURI().getQuery();
        httpExchange.sendResponseHeaders(204, -1);

        Map<String, String> args = Arrays.stream(query.split("&"))
                .map(s -> s.split("=", 2)).collect(
                        HashMap::new, (map, strArray) -> map.put(strArray[0], strArray[1]), HashMap::putAll
                );

        String state = args.get("state");
        if (authSequence.containsKey(state)) {
            String code = args.get("code");
            String baseURL = "%s?client_id=%s&client_secret=%s&code=%s&grant_type=authorization_code"
                    .formatted(tokenGrantURL, clientID, clientSecret, code);
            if (redirect != null)
                baseURL += "&" + redirect;
            HttpPost post = new HttpPost(baseURL);
            post.setHeader("Accept", "application/json");

        }
    }
}
