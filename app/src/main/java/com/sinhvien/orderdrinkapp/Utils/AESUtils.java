package com.sinhvien.orderdrinkapp.Utils;

import android.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class AESUtils {
    // 16-byte key for AES-128
    private static final byte[] keyValue = new byte[]{'R', 'o', 'y', 'a', 'l', 'P', 'O', 'S', 'S', 'e', 'c', 'r', 'e', 't', '1', '2'};

    public static String encrypt(String cleartext) throws Exception {
        SecretKeySpec key = new SecretKeySpec(keyValue, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encVal = cipher.doFinal(cleartext.getBytes());
        return Base64.encodeToString(encVal, Base64.DEFAULT);
    }

    public static String decrypt(String encrypted) throws Exception {
        SecretKeySpec key = new SecretKeySpec(keyValue, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decordedValue = Base64.decode(encrypted, Base64.DEFAULT);
        byte[] decValue = cipher.doFinal(decordedValue);
        return new String(decValue);
    }
}
