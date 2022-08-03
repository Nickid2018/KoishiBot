package io.github.nickid2018.koishibot.module.github;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.nickid2018.koishibot.message.api.AbstractMessage;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.server.ServerManager;
import io.github.nickid2018.koishibot.util.AsyncUtil;
import io.github.nickid2018.koishibot.util.JsonUtil;
import io.github.nickid2018.koishibot.util.MessageUtil;
import io.github.nickid2018.koishibot.util.WebUtil;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class GitHubAuthenticator implements HttpHandler {

    private String githubToken;
    private String githubOauth2ClientId;
    private String githubOauth2ClientSecret;

    private final Map<String, Consumer<String>> authSequence = new ConcurrentHashMap<>();

    protected GitHubAuthenticator() {
        try {
            ServerManager.addHandle("/githubOAuth", this);
        } catch (Exception e) {
            GitHubModule.GITHUB_LOGGER.error("Cannot start GitHub OAuth 2.0 Service.", e);
        }
    }

    protected void readSettings(JsonObject settingsRoot) {
        githubToken = JsonUtil.getString(settingsRoot, "github_token").orElse("");
        JsonUtil.getData(settingsRoot, "github_oauth2", JsonObject.class).ifPresent(oauth -> {
            githubOauth2ClientId = JsonUtil.getStringOrNull(oauth, "client_id");
            githubOauth2ClientSecret = JsonUtil.getStringOrNull(oauth, "client_secret");
        });
    }

    public boolean enableOAuth2() {
        return githubOauth2ClientId != null;
    }

    public HttpUriRequest authenticate(HttpUriRequest request) {
        if (githubToken != null && !githubToken.isEmpty())
            request.addHeader("Authorization", "token " + githubToken);
        return request;
    }

    public HttpUriRequest acceptGitHubJSON(HttpUriRequest request, String token) {
        request.addHeader("Authorization", "token " + token);
        request.addHeader("Accept", "application/vnd.github.v3+json");
        return request;
    }

    public HttpUriRequest acceptGitHubJSON(HttpUriRequest request) {
        request.addHeader("Accept", "application/vnd.github.v3+json");
        return authenticate(request);
    }

    public void authenticateOperation(Consumer<String> operation,
                                             MessageContext context, Environment environment, String... needScopes) {
        if (githubOauth2ClientId == null) {
            AbstractMessage message = environment.newText(
                    "警告: 此操作需要授权，请发送私人访问令牌用于授权。\n" +
                            "本次操作需要" + String.join(", ", needScopes) + "权限。"
            );
            AsyncUtil.execute(() -> environment.getMessageSender().sendMessageAwait(context, message, (sent, reply) -> {
                String token = MessageUtil.getFirstText(reply);
                if (token != null && !token.equalsIgnoreCase("N"))
                    AsyncUtil.execute(() -> operation.accept(token));
                else
                    environment.getMessageSender().sendMessage(context, environment.newText("已取消授权"));
            }));
        } else {
            AbstractMessage message = environment.newText(
                    "警告: 此操作需要授权，请输入您的用户名。"
            );
            AsyncUtil.execute(() -> environment.getMessageSender().sendMessageAwait(context, message, (sent, reply) -> {
                String name = MessageUtil.getFirstText(reply);
                if (name != null) {
                    try {
                        String state = randomState();
                        String url = "https://github.com/login/oauth/authorize?client_id=" + githubOauth2ClientId
                                + "&login=" + name + "&scope=" + URLEncoder.encode(String.join(" ", needScopes), StandardCharsets.UTF_8)
                                + "&state=" + state;
                        authSequence.put(state, operation);

                        AsyncUtil.execute(() -> environment.getMessageSender().sendMessage(context, environment.newText(
                                "使用OAuth验证，请点击下方链接。\n" + url
                        )));
                    } catch (Exception e) {
                        environment.getMessageSender().onError(e, "github.oauth", context, false);
                    }
                } else
                    environment.getMessageSender().sendMessage(context, environment.newText("已取消授权"));
            }));
        }
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
            HttpPost post = new HttpPost("https://github.com/login/oauth/access_token?client_id="
                    + githubOauth2ClientId + "&client_secret=" + githubOauth2ClientSecret
                    + "&code=" + code);
            post.setHeader("Accept", "application/json");
            JsonObject object = WebUtil.fetchDataInJson(post).getAsJsonObject();

            String token = JsonUtil.getStringOrNull(object, "access_token");
            Consumer<String> authData = authSequence.remove(state);

            authData.accept(token);
        }
    }
}
