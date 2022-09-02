package io.github.nickid2018.koishibot.module.github;

import com.google.gson.JsonObject;
import io.github.nickid2018.koishibot.message.api.AbstractMessage;
import io.github.nickid2018.koishibot.message.api.Environment;
import io.github.nickid2018.koishibot.message.api.MessageContext;
import io.github.nickid2018.koishibot.util.AsyncUtil;
import io.github.nickid2018.koishibot.util.JsonUtil;
import io.github.nickid2018.koishibot.util.MessageUtil;
import io.github.nickid2018.koishibot.util.web.OAuth2Authenticator;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class GitHubAuthenticator {

    private String githubToken;

    private OAuth2Authenticator authenticator;

    protected void readSettings(JsonObject settingsRoot) {
        if (authenticator != null) {
            authenticator.close();
            authenticator = null;
        }
        githubToken = JsonUtil.getString(settingsRoot, "github_token").orElse("");
        JsonUtil.getData(settingsRoot, "github_oauth2", JsonObject.class).ifPresent(oauth -> {
            try {
                authenticator = new OAuth2Authenticator(
                        "github", "https://github.com/login/oauth/authorize",
                        "https://github.com/login/oauth/access_token", null, true,
                        "/githubOAuth", JsonUtil.getStringOrNull(oauth, "client_id"),
                        JsonUtil.getStringOrNull(oauth, "client_secret"), false
                );
            } catch (IOException e) {
                GitHubModule.GITHUB_LOGGER.error("Cannot start GitHub OAuth 2.0 Service.", e);
                authenticator = null;
            }
        });
    }

    public boolean enableOAuth2() {
        return authenticator != null;
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
        if (enableOAuth2()) {
            AbstractMessage message = environment.newText(
                    "警告: 此操作需要授权，请输入您的用户名。"
            );
            AsyncUtil.execute(() -> environment.getMessageSender().sendMessageAwait(context, message, (sent, reply) -> {
                String name = MessageUtil.getFirstText(reply);
                if (name != null)
                    authenticator.authenticate(name,
                            str -> environment.getMessageSender().sendMessage(context, environment.newText("请点击链接授权：\n" + str)),
                            operation, List.of(), Map.of("login", name));
                else
                    environment.getMessageSender().sendMessage(context, environment.newText("已取消授权"));
            }));
        } else {
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
        }
    }
}
