package io.github.nickid2018.koishibot.network;

public class StringData implements SerializableData {

    public String data;

    public StringData() {
    }

    public StringData(String data) {
        this.data = data;
    }

    @Override
    public void read(ByteData buf) {
        data = buf.readString();
    }

    @Override
    public void write(ByteData buf) {
        buf.writeString(data);
    }
}
