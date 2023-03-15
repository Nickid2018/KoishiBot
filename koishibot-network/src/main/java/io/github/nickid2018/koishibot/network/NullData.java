package io.github.nickid2018.koishibot.network;

public class NullData implements SerializableData {

    public static final NullData INSTANCE = new NullData();

    private NullData() {
    }

    @Override
    public void read(ByteData buf) {
    }

    @Override
    public void write(ByteData buf) {
    }
}
