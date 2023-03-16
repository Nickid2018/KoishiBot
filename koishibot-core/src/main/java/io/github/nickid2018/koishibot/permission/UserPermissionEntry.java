package io.github.nickid2018.koishibot.permission;

import java.io.Serializable;

public record UserPermissionEntry(String user, PermissionLevel level, long expired) implements Serializable {

    public static UserPermissionEntry permanentPermission(String user, PermissionLevel level) {
        return new UserPermissionEntry(user, level, Long.MAX_VALUE);
    }

    public boolean isExpired() {
        return expired < System.currentTimeMillis();
    }
}
