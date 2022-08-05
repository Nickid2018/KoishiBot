package io.github.nickid2018.koishibot.util.web;

import java.io.IOException;

public class ErrorCodeException extends IOException {

    public final int code;

    public ErrorCodeException(int code) {
        this.code = code;
    }
}
