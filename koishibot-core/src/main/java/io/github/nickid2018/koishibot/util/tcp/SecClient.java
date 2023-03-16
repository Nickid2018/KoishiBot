package io.github.nickid2018.koishibot.util.tcp;

import javax.crypto.spec.SecretKeySpec;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.function.Consumer;

public class SecClient {

    private final Socket socket;
    private final DataInputStream dataInput;
    private final DataOutputStream dataOutput;

    private final Consumer<byte[]> dataListener;
    private volatile Key aes;

    public SecClient(InetSocketAddress addr, Consumer<byte[]> dataListener) throws IOException {
        socket = new Socket(addr.getAddress(), addr.getPort());
        dataInput = new DataInputStream(socket.getInputStream());
        dataOutput = new DataOutputStream(socket.getOutputStream());
        this.dataListener = dataListener;
        Thread clientThread = new Thread(this::run, "Client");
        clientThread.setDaemon(true);
        clientThread.start();
    }

    private void run() {
        try {
            KeyPair pair = CryptUtil.generateRSAKey();
            PublicKey minePublic = pair.getPublic();
            PrivateKey minePrivate = pair.getPrivate();

            // Swap Public
            byte[] otherPublic = readInternal(null);
            EncodedKeySpec encodedKeySpec = new X509EncodedKeySpec(otherPublic);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey remotePublic = keyFactory.generatePublic(encodedKeySpec);
            sendInternal(minePublic.getEncoded(), null);

            // Verify
            byte[] nonce = readInternal(minePrivate);
            sendInternal(nonce, remotePublic);

            // AES Key
            byte[] key = readInternal(minePrivate);
            aes = new SecretKeySpec(key, "AES");

            while (true)
                dataListener.accept(readInternal(aes));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void send(byte[] data) throws Exception {
        if (socket.isClosed())
            throw new IOException("Connection broke.");
        if (aes != null)
            sendInternal(data, aes);
    }

    private void sendInternal(byte[] data, Key key) throws Exception {
        byte[] encode = CryptUtil.encrypt(data, key);
        dataOutput.writeInt(encode.length);
        dataOutput.write(encode);
    }

    private byte[] readInternal(Key key) throws Exception {
        int length = dataInput.readInt();
        byte[] dataEncrypted = new byte[length];
        if (length != dataInput.read(dataEncrypted))
            throw new EOFException();
        return CryptUtil.decrypt(dataEncrypted, key);
    }

    public void close() throws IOException {
        socket.close();
    }
}
