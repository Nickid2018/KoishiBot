package io.github.nickid2018.koishibot.util;

public class MutableBoolean {

    private boolean value;

    public MutableBoolean(boolean val) {
        value = val;
    }

    public boolean getValue() {
        return value;
    }

    public void setValue(boolean val) {
        value = val;
    }
}
