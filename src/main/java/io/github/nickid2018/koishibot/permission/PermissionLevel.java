package io.github.nickid2018.koishibot.permission;

public enum PermissionLevel {
    OWNER,
    ADMIN,
    TRUSTED,
    UNTRUSTED,
    BANNED;

    public boolean levelGreater(PermissionLevel other) {
        return ordinal() < other.ordinal();
    }
    public boolean levelGreaterOrEquals(PermissionLevel other) {
        return ordinal() <= other.ordinal();
    }
}
