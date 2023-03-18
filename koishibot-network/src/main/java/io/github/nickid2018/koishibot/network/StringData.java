package io.github.nickid2018.koishibot.network;

public class StringData implements SerializableData {

    private String str;

    public StringData() {
    }

    public StringData(String str) {
        this.str = str;
    }

    public String getStr() {
        return str;
    }

    @Override
    public void read(ByteData buf) {
        buf.writeString(str);
    }

    @Override
    public void write(ByteData buf) {
        str = buf.readString();
    }
}
