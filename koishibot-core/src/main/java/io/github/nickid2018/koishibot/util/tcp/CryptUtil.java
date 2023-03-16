package io.github.nickid2018.koishibot.util.tcp;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

public class CryptUtil {

    public static byte[] encrypt(byte[] data, Key key) throws Exception {
        if (key == null)
            return data;
        Cipher cipher = Cipher.getInstance(key.getAlgorithm());
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    public static byte[] decrypt(byte[] data, Key key) throws Exception {
        if (key == null)
            return data;
        Cipher cipher = Cipher.getInstance(key.getAlgorithm());
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    public static KeyPair generateRSAKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        return generator.generateKeyPair();
    }

    public static Key generateAESKey() throws Exception {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(128);
        return generator.generateKey();
    }
}
