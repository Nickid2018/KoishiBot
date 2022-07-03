package io.github.nickid2018.koishibot.util.value;

public class MutableInt {

    private int val;

    public MutableInt(int val) {
        this.val = val;
    }

    public void setValue(int val) {
        this.val = val;
    }

    public int getValue() {
        return val;
    }

    public int getAndIncrease() {
        return val++;
    }
}
