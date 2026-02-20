package org.blackum.blackaddons.core.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class EncryptionUtils {

    // Key is fetched from bot at runtime
    // Smh free rat
    private static SecretKeySpec secretKey;

    public static void setKeyBase64(String base64Key) {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(base64Key);
            secretKey = new SecretKeySpec(decodedKey, "AES");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String encrypt(String strToEncrypt) {
        try {
            if (secretKey == null)
                return null;
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            System.err.println("Error while encrypting: " + e.toString());
        }
        return null;
    }

    public static String decrypt(String strToDecrypt) {
        try {
            if (secretKey == null)
                return null;
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
        } catch (Exception e) {
            System.err.println("Error while decrypting: " + e.toString());
        }
        return null;
    }
}
