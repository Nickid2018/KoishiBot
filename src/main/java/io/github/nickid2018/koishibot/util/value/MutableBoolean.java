package io.github.nickid2018.koishibot.util.value;

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

    public void or(boolean val) {
        value |= val;
    }
}
