package io.github.nickid2018.koishibot.monitor;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class GitHubApiTests {

    @Test
    public void testWorkflow() throws IOException, NoSuchAlgorithmException {
        byte[] data = MessageDigest.getInstance("SHA-256").digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02x", b));
        }
        System.out.println(sb.toString());
//        HttpGet get = new HttpGet("https://api.github.com/repos/Nickid2018/Koishibot/actions/runs/4436923642/artifacts");
//        WebRequests.addGitHubHeaders(get);
//        System.out.println(WebRequests.fetchDataInJson(get));
    }
}
