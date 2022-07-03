package io.github.nickid2018.koishibot.message.api;

public interface UserInfo extends ContactInfo {

    String getUserId();

    boolean isStranger();

    void nudge(ContactInfo contact);
}
