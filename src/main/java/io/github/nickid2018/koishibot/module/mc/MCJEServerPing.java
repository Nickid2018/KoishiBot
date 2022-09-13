package io.github.nickid2018.koishibot.module.mc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

/*
 * @author zh32 <zh32 at zh32.de>, modified by Nickid2018
 */
public record MCJEServerPing(InetSocketAddress host) {

    public static final int TIMEOUT = 10000;

    private static int readVarInt(DataInputStream in) throws IOException {
        int i = 0;
        int j = 0;
        while (true) {
            int k = in.readByte();
            i |= (k & 0x7F) << j++ * 7;
            if (j > 5)
                throw new IOException("VarInt too big");
            if ((k & 0x80) != 128) break;
        }
        return i;
    }

    private static void writeVarInt(DataOutputStream out, int paramInt) throws IOException {
        while (true) {
            if ((paramInt & 0xFFFFFF80) == 0) {
                out.writeByte(paramInt);
                return;
            }
            out.writeByte(paramInt & 0x7F | 0x80);
            paramInt >>>= 7;
        }
    }

    public JsonObject fetchData() throws IOException {
        Socket socket = new Socket();
        OutputStream outputStream;
        DataOutputStream dataOutputStream;
        InputStream inputStream;
        InputStreamReader inputStreamReader;

        socket.setSoTimeout(TIMEOUT);
        socket.connect(host, TIMEOUT);

        outputStream = socket.getOutputStream();
        dataOutputStream = new DataOutputStream(outputStream);

        inputStream = socket.getInputStream();
        inputStreamReader = new InputStreamReader(inputStream);

        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream handshake = new DataOutputStream(b);

        handshake.writeByte(0x00); // packet id for handshake
        writeVarInt(handshake, 4); // protocol version
        writeVarInt(handshake, host.getHostString().length()); // host length
        handshake.writeBytes(host.getHostString()); // host string
        handshake.writeShort(host.getPort()); // port
        writeVarInt(handshake, 1); // state (1 for handshake)

        writeVarInt(dataOutputStream, b.size()); // prepend size
        dataOutputStream.write(b.toByteArray()); // write handshake packet

        dataOutputStream.writeByte(0x01); // size is only 1
        dataOutputStream.writeByte(0x00); // packet id for ping
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        readVarInt(dataInputStream); // size of packet, ignored
        int id = readVarInt(dataInputStream); // packet id

        if (id == -1)
            throw new IOException("Premature end of stream.");
        if (id != 0x00) // we want a status response
            throw new IOException("Invalid packetID: expect 0, but found " + id);

        int length = readVarInt(dataInputStream); // length of json string

        if (length == -1)
            throw new IOException("Premature end of stream.");
        if (length == 0)
            throw new IOException("Invalid string length.");

        byte[] in = new byte[length];
        dataInputStream.readFully(in);  // read json string
        String json = new String(in);

        dataOutputStream.writeByte(0x09); // size of packet
        dataOutputStream.writeByte(0x01); // 0x01 for ping
        dataOutputStream.writeLong(System.currentTimeMillis()); // current time

        readVarInt(dataInputStream);
        id = readVarInt(dataInputStream);

        if (id == -1)
            throw new IOException("Premature end of stream.");
        if (id != 0x01)
            throw new IOException("Invalid packetID: expect 1, but found " + id);

        long pingTime = dataInputStream.readLong(); // read response

        dataOutputStream.close();
        outputStream.close();
        inputStreamReader.close();
        inputStream.close();
        socket.close();

        JsonObject object = JsonParser.parseString(json).getAsJsonObject();
        object.addProperty("ping", System.currentTimeMillis() - pingTime);

        return object;
    }
}
