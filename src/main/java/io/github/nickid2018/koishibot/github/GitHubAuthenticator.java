package io.github.nickid2018.koishibot.github;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.nickid2018.koishibot.message.api.AbstractMessage;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.message.api.TextMessage;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GitHubAuthenticator implements HttpHandler {

    public static final GitHubAuthenticator AUTHENTICATOR = new GitHubAuthenticator();

    private final Map<String, Consumer<String>> authSequence = new ConcurrentHashMap<>();

    private GitHubAuthenticator() {
        try {
            ServerManager.addHandle("/githubOAuth", this);
        } catch (Exception e) {
            GitHubListener.GITHUB_LOGGER.error("Cannot start GitHub OAuth 2.0 Service.", e);
        }
    }

    public static HttpUriRequest authenticate(HttpUriRequest request) {
        if (GitHubListener.GITHUB_TOKEN != null && !GitHubListener.GITHUB_TOKEN.isEmpty())
            request.addHeader("Authorization", "token " + GitHubListener.GITHUB_TOKEN);
        return request;
    }

    public static HttpUriRequest acceptGitHubJSON(HttpUriRequest request, String token) {
        request.addHeader("Authorization", "token " + token);
        request.addHeader("Accept", "application/vnd.github.v3+json");
        return request;
    }

    public static HttpUriRequest acceptGitHubJSON(HttpUriRequest request) {
        request.addHeader("Accept", "application/vnd.github.v3+json");
        return authenticate(request);
    }

    public static void authenticateOperation(Consumer<String> operation,
                                             MessageContext context, Environment environment, String... needScopes) {
        if (GitHubListener.GITHUB_OAUTH2_CLIENT_ID == null) {
            AbstractMessage message = environment.newText(
                    "??????: ??????????????????????????????????????????????????????????????????\n" +
                            "??????????????????" + String.join(", ", needScopes) + "?????????"
            );
            AsyncUtil.execute(() -> environment.getMessageSender().sendMessageAwait(context, message, (sent, reply) -> {
                String token = MessageUtil.getFirstText(reply);
                if (token != null && !token.equalsIgnoreCase("N"))
                    AsyncUtil.execute(() -> operation.accept(token));
                else
                    environment.getMessageSender().sendMessage(context, environment.newText("???????????????"));
            }));
        } else {
            AbstractMessage message = environment.newText(
                    "??????: ???????????????????????????????????????????????????"
            );
            AsyncUtil.execute(() -> environment.getMessageSender().sendMessageAwait(context, message, (sent, reply) -> {
                List<TextMessage> texts = Stream.of(reply.getMessages())
                        .filter(m -> m instanceof TextMessage).map(m -> (TextMessage) m).collect(Collectors.toList());
                if (texts.size() == 1) {
                    try {
                        String name = texts.get(0).getText();
                        String state = randomState();
                        String url = "https://github.com/login/oauth/authorize?client_id=" + GitHubListener.GITHUB_OAUTH2_CLIENT_ID
                                + "&login=" + name + "&scope=" + URLEncoder.encode(String.join(" ", needScopes), StandardCharsets.UTF_8)
                                + "&state=" + state;
                        AUTHENTICATOR.authSequence.put(state, operation);

                        AsyncUtil.execute(() -> environment.getMessageSender().sendMessage(context, environment.newText(
                                "??????OAuth?????????????????????????????????\n" + url
                        )));
                    } catch (Exception e) {
                        environment.getMessageSender().onError(e, "github.oauth", context, false);
                    }
                } else
                    environment.getMessageSender().sendMessage(context, environment.newText("???????????????"));
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
                    + GitHubListener.GITHUB_OAUTH2_CLIENT_ID + "&client_secret=" + GitHubListener.GITHUB_OAUTH2_CLIENT_SECRET
                    + "&code=" + code);
            post.setHeader("Accept", "application/json");
            JsonObject object = WebUtil.fetchDataInJson(post).getAsJsonObject();

            String token = JsonUtil.getStringOrNull(object, "access_token");
            Consumer<String> authData = authSequence.remove(state);

            authData.accept(token);
        }
    }
}
