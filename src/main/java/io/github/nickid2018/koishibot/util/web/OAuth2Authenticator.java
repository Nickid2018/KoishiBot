package io.github.nickid2018.koishibot.util.web;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.nickid2018.koishibot.core.Settings;
import io.github.nickid2018.koishibot.server.ServerManager;
import io.github.nickid2018.koishibot.util.AsyncUtil;
import io.github.nickid2018.koishibot.util.DataReader;
import io.github.nickid2018.koishibot.util.JsonUtil;
import kotlin.Triple;
import org.apache.http.client.methods.HttpPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class OAuth2Authenticator implements HttpHandler {

    public static final Logger OAUTH2_LOGGER = LoggerFactory.getLogger("OAuth 2.0 Service");

    private final String oauthName;
    private final String authenticateURL;
    private final String tokenGrantURL;
    private final boolean refreshTokenEnabled;
    private final String redirect;
    private final String clientID;
    private final String clientSecret;
    private final boolean uriAppend;
    private final DataReader<Map<String, AuthenticateToken>> dataReader;

    private final Map<String, Triple<String, List<String>, Consumer<String>>> authSequence = new ConcurrentHashMap<>();

    public OAuth2Authenticator(String oauthName,
                               String authenticateURL, String tokenGrantURL, boolean refreshTokenEnabled,
                               String redirect, String clientID, String clientSecret, boolean uriAppend) throws IOException {
        this.oauthName = oauthName;
        this.authenticateURL = authenticateURL;
        this.tokenGrantURL = tokenGrantURL;
        this.refreshTokenEnabled = refreshTokenEnabled;
        this.redirect = redirect;
        this.clientID = clientID;
        this.clientSecret = clientSecret;
        this.uriAppend = uriAppend;
        dataReader = refreshTokenEnabled ? new DataReader<>(
                new File("oauth2/" + oauthName + ".dat"), HashMap::new
        ) : null;
        ServerManager.addHandle(redirect, this);
    }

    public void authenticate(String user, Consumer<String> authURLSender, Consumer<String> operation,
                             List<String> scopes,  Map<String, String> extraParameters) {
        AuthenticateToken token = dataReader.getDataSilently().get(user);
        boolean usable = token != null;

        for (String scope : scopes) {
            if (!usable)
                break;
            usable = token.scopes().contains(scope);
        }

        if (token != null && !usable)
            scopes.addAll(token.scopes());

        if (refreshTokenEnabled && usable) {
            AsyncUtil.execute(() -> {
                try {
                    String accessToken = token.accessToken();
                    if (token.isExpired())
                            accessToken = authenticateRefresh(user);
                    String finalAccessToken = accessToken;
                    AsyncUtil.execute(() -> operation.accept(finalAccessToken));
                } catch (IOException e) {
                    OAUTH2_LOGGER.error("Can't refresh access token. Name = " + oauthName + ", User = " + user, e);
                    authenticateCode(user, authURLSender, operation, scopes, extraParameters);
                }
            });
        } else
            authenticateCode(user, authURLSender, operation, scopes, extraParameters);
    }

    private String authenticateRefresh(String user) throws IOException {
        AuthenticateToken token = dataReader.getData().get(user);
        String url = "%s?grant_type=refresh_token&refresh_token=%s".formatted(tokenGrantURL, token.refreshToken());

        HttpPost post = new HttpPost(url);
        post.setHeader("Accept", "application/json");
        JsonObject object = WebUtil.fetchDataInJson(post).getAsJsonObject();

        String accessToken = JsonUtil.getStringOrNull(object, "access_token");
        String refreshToken = JsonUtil.getStringOrNull(object, "refresh_token");
        long expiredTime = JsonUtil.getIntOrZero(object, "expires_in") * 1000L + System.currentTimeMillis();

        token = new AuthenticateToken(accessToken, expiredTime, refreshToken, token.scopes());
        dataReader.getData().put(user, token);
        dataReader.saveData();

        return accessToken;
    }

    private void authenticateCode(String user, Consumer<String> authURLSender, Consumer<String> operation,
                                  List<String> scopes,  Map<String, String> extraParameters) {
        String state = randomState();
        String url = "%s?client_id=%s&state=%s&scope=%s&response_type=code".formatted(
                authenticateURL, clientID, state, WebUtil.encode(String.join(",", scopes)));
        if (uriAppend)
            url += "&redirect_uri=http://%s%s".formatted(Settings.LOCAL_IP, redirect);

        String extra = extraParameters.entrySet().stream()
                .map(en -> WebUtil.encode(en.getKey()) + "=" + WebUtil.encode(en.getValue()))
                .collect(Collectors.joining("&"));

        if (!extra.isEmpty())
            url += "&" + extra;

        authSequence.put(state, new Triple<>(user, scopes, operation));
        authURLSender.accept(url);
    }

    private static String randomState() {
        UUID uuid = UUID.randomUUID();
        return Long.toHexString(uuid.getMostSignificantBits()) + Long.toHexString(uuid.getLeastSignificantBits());
    }

    @Override
    public void handle(HttpExchange httpExchange) {
        try {
            String query = httpExchange.getRequestURI().getQuery();
            httpExchange.sendResponseHeaders(204, -1);

            Map<String, String> args = Arrays.stream(query.split("&"))
                    .map(s -> s.split("=", 2)).collect(
                            HashMap::new, (map, strArray) -> map.put(strArray[0], strArray[1]), HashMap::putAll
                    );

            String state = args.get("state");
            if (authSequence.containsKey(state)) {
                OAUTH2_LOGGER.info("Received code from state {}.", state);

                String code = args.get("code");
                String url = "%s?client_id=%s&client_secret=%s&code=%s&grant_type=authorization_code"
                        .formatted(tokenGrantURL, clientID, clientSecret, code);
                if (uriAppend)
                    url += "&redirect_uri=http://%s%s".formatted(Settings.LOCAL_IP, redirect);
                HttpPost post = new HttpPost(url);
                post.setHeader("Accept", "application/json");
                JsonObject object = WebUtil.fetchDataInJson(post).getAsJsonObject();

                String accessToken = JsonUtil.getStringOrNull(object, "access_token");
                Triple<String, List<String>, Consumer<String>> authData = authSequence.remove(state);

                if (refreshTokenEnabled) {
                    String refreshToken = JsonUtil.getStringOrNull(object, "refresh_token");
                    long expiredTime = JsonUtil.getIntOrZero(object, "expires_in") * 1000L + System.currentTimeMillis();
                    AuthenticateToken token = new AuthenticateToken(accessToken, expiredTime, refreshToken, authData.getSecond());
                    dataReader.getData().put(authData.getFirst(), token);
                    dataReader.saveData();
                }

                authData.getThird().accept(accessToken);
            }
        } catch (Exception e) {
            OAUTH2_LOGGER.error("Cannot authenticate.", e);
        }
    }

    public void close() {
        ServerManager.removeHandle(redirect);
    }
}
