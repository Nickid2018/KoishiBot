package io.github.nickid2018.koishibot.monitor;

import org.junit.jupiter.api.Test;

import java.io.IOException;

public class GitHubApiTests {

    @Test
    public void testWorkflow() throws IOException {
        Settings.ACTION_REPO = "Nickid2018/KoishiBot;build.yml";
        System.out.println(GitHubWebRequests.getArtifacts(GitHubWebRequests.getNowActionID()));
    }
}
