package io.github.nickid2018.koishibot.monitor;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class GitHubApiTests {

    @Test
    public void testWorkflow() throws IOException {
        HttpGet get = new HttpGet("https://api.github.com/repos/Nickid2018/Koishibot/actions/runs/4436923642/artifacts");
        WebRequests.addGitHubHeaders(get);
        System.out.println(WebRequests.fetchDataInJson(get));
    }
}
