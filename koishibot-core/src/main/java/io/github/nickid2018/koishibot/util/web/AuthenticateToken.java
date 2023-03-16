package io.github.nickid2018.koishibot.util.web;

import java.io.Serializable;
import java.util.List;

public record AuthenticateToken(String accessToken,
                                long expireTime,
                                String refreshToken,
                                List<String> scopes) implements Serializable {

    public boolean isExpired() {
        return expireTime < System.currentTimeMillis();
    }
}
