package icu.niunai.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

public class AESUtil {


    public static String AESDecode(String token) throws GeneralSecurityException {
        byte[] key = "a=s]u-dK'U-HG=j|y-FGjY.F;F=Y@J~G".getBytes(StandardCharsets.UTF_8);
        byte[] input = AESUtil.parseHexStr2Byte(token);
        assert input != null;
        byte[] newTokenSecret = AESUtil.decrypt(key, input);

        return new String(newTokenSecret, StandardCharsets.UTF_8);
    }

    /**
     * 传入待加密字符串，返回加密后字符串（字符数组转16进制后）
     *
     * @param token 待加密token
     * @return  加密token
     */
    public static String AESEncode(String token) throws GeneralSecurityException {
        //进行加密(更新）
        byte[] key = "a=s]u-dK'U-HG=j|y-FGjY.F;F=Y@J~G".getBytes(StandardCharsets.UTF_8);
        byte[] input = token.getBytes(StandardCharsets.UTF_8);
        byte[] newTokenSecret = AESUtil.encrypt(key, input);
        return AESUtil.parseByte2HexStr(newTokenSecret);
    }

    /**
     * 加密
     *
     * @param key 钥匙
     * @param input 待加密字符
     * @return 加密字符
     */
    public static byte[] encrypt(byte[] key, byte[] input) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        // CBC模式需要生成一个16 bytes的initialization vector:
        SecureRandom sr = SecureRandom.getInstanceStrong();
        byte[] iv = sr.generateSeed(16);
        IvParameterSpec ivps = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivps);
        byte[] data = cipher.doFinal(input);
        // IV不需要保密，把IV和密文一起返回:
        return join(iv, data);
    }

    /**
     * 解密
     *
     * @param key 钥匙
     * @param input 待解密字符
     * @return 结果
     */
    public static byte[] decrypt(byte[] key, byte[] input) throws GeneralSecurityException {
        // 把input分割成IV和密文:
        byte[] iv = new byte[16];
        byte[] data = new byte[input.length - 16];
        System.arraycopy(input, 0, iv, 0, 16);
        System.arraycopy(input, 16, data, 0, data.length);
        // 解密:
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec ivps = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivps);
        return cipher.doFinal(data);
    }

    public static byte[] join(byte[] bs1, byte[] bs2) {
        byte[] r = new byte[bs1.length + bs2.length];
        System.arraycopy(bs1, 0, r, 0, bs1.length);
        System.arraycopy(bs2, 0, r, bs1.length, bs2.length);
        return r;
    }

    /**
     * 将二进制转换成16进制
     *
     * @param buf 待转换二进制数组
     * @return 16进制字符串
     */
    public static String parseByte2HexStr(byte[] buf) {
        StringBuilder sb = new StringBuilder();
        for (byte b : buf) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString();
    }

    /**
     * 将16进制转换为二进制
     *
     * @param hexStr 待转换16进制字符串
     * @return 2进制数组结果
     */
    public static byte[] parseHexStr2Byte(String hexStr) {
        if (hexStr.isEmpty())
            return null;
        byte[] result = new byte[hexStr.length() / 2];
        for (int i = 0; i < hexStr.length() / 2; i++) {
            int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
            int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2), 16);
            result[i] = (byte) (high * 16 + low);
        }
        return result;
    }

}
