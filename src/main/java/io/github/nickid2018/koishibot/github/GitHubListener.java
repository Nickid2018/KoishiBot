package io.github.nickid2018.koishibot.github;

import io.github.nickid2018.koishibot.KoishiBotMain;
import io.github.nickid2018.koishibot.core.Settings;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class GitHubListener {

    private static final Map<String, Date> UPDATE_DATA = new HashMap<>();

    public static void authenticate(HttpUriRequest request) {
        request.addHeader("Authorization", "token " + Settings.GITHUB_TOKEN);
    }

    public static void acceptJSON(HttpUriRequest request) {
        authenticate(request);
        request.addHeader("Accept", "application/vnd.github.v3+json");
    }

    static {
        File githubFile = KoishiBotMain.INSTANCE.resolveConfigFile("botKoishi.json");
    }
}
