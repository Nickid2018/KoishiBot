package io.github.nickid2018.koishibot.message.api;

public interface UserInfo extends ContactInfo {

    String getUserId();

    void nudge(ContactInfo contact);
}
