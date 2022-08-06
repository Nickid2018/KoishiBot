package io.github.nickid2018.koishibot.util.web;

import java.util.List;

public record AuthenticateToken(String accessToken, long expireTime, String refreshToken, List<String> scopes) {

    public boolean isExpired() {
        return expireTime < System.currentTimeMillis();
    }
}
