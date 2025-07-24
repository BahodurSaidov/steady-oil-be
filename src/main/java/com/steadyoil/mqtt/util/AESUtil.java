package com.steadyoil.mqtt.util;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

@CommonsLog
@Service
public class AESUtil {

    private static String baseId;
    private static String aesPassword;
    private static String aesSalt;
    private static String aesAlgorithm;
    private static byte[] aesIv;

    public static SecretKey generateKey(int n) throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(n);
        SecretKey key = keyGenerator.generateKey();
        return key;
    }

    public static String getApiKey(String sensorId) {
        return encryptedString(sensorId);
    }

    public static String encryptedString(String sensorId) {
        String string = baseId + sensorId;
        SecretKey key = null;
        String cipherText = "";
        IvParameterSpec ivParameterSpec = null;

        try {
            key = AESUtil.getKeyFromPassword(aesPassword, aesSalt);
            ivParameterSpec = AESUtil.generateIv();
            cipherText = AESUtil.encrypt(aesAlgorithm, string, key, ivParameterSpec);
        } catch (Exception e) {
            log.debug(String.format("encrypt... aesPassword: %s, aesSalt: %s, aesAlgorithm: %s", aesPassword, aesSalt, aesAlgorithm));
            log.error(String.format("AESUtil encrypt: %s", e.getMessage()));
            throw new Error("AESUtil encrypt: could not encrypt.");
        }

        return cipherText;
    }

    public static String encrypt(String algorithm, String input, SecretKey key, IvParameterSpec iv) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        byte[] cipherText = cipher.doFinal(input.getBytes());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(cipherText);
    }

    public static IvParameterSpec generateIv() {
        return new IvParameterSpec(aesIv);
    }

    public static SecretKey getKeyFromPassword(String password, String salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536, 256);
        SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        return secret;
    }

    public static String getSensorId(String apiKey) {
        return decryptedString(apiKey).substring(baseId.length());
    }

    public static String decryptedString(String cipherText) {
        SecretKey key = null;
        String plainText = "";
        IvParameterSpec ivParameterSpec = null;

        try {
            key = AESUtil.getKeyFromPassword(aesPassword, aesSalt);
            ivParameterSpec = AESUtil.generateIv();
            plainText = AESUtil.decrypt(aesAlgorithm, cipherText, key, ivParameterSpec);
        } catch (Exception e) {
            log.debug(String.format("decrypt... aesPassword: %s, aesSalt: %s, aesAlgorithm: %s", aesPassword, aesSalt, aesAlgorithm));
            log.error(String.format("AESUtil decrypt: %s", e.getMessage()));
            throw new Error("AESUtil decrypt: Invalid key provided.");
        }

        return plainText;
    }

    public static String decrypt(String algorithm, String cipherText, SecretKey key, IvParameterSpec iv) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        byte[] plainText = cipher.doFinal(Base64.getUrlDecoder().decode(cipherText));
        return new String(plainText);
    }

    @Value("${com.steadyoil.aes.base_id}")
    public void setAesBaseId(String baseId) {
        AESUtil.baseId = baseId;
    }

    @Value("${com.steadyoil.aes.password}")
    public void setAesPassword(String aesPassword) {
        AESUtil.aesPassword = aesPassword;
    }

    @Value("${com.steadyoil.aes.salt}")
    public void setAesSalt(String aesSalt) {
        AESUtil.aesSalt = aesSalt;
    }

    @Value("${com.steadyoil.aes.algorithm}")
    public void setAesAlgorithm(String aesAlgorithm) {
        AESUtil.aesAlgorithm = aesAlgorithm;
    }

    @Value("${com.steadyoil.aes.iv}")
    public void setAesIv(byte[] aesIv) {
        AESUtil.aesIv = aesIv;
    }
}
