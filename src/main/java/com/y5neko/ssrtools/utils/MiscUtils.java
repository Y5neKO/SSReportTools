package com.y5neko.ssrtools.utils;

import java.security.SecureRandom;

public class MiscUtils {
    private static final String HEX_CHARS = "0123456789ABCDEF";
    private static final String NUM_CHARS = "0123456789";
    private static final SecureRandom random = new SecureRandom();

    // 生成paraID
    public static String getParaID() {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(HEX_CHARS.charAt(random.nextInt(HEX_CHARS.length())));
        }
        return sb.toString();
    }

    // 生成TocName
    public static String getTocName() {
        StringBuilder sb = new StringBuilder(9);
        for (int i = 0; i < 8; i++) {
            sb.append(NUM_CHARS.charAt(random.nextInt(NUM_CHARS.length())));
        }
        return "_Toc" + sb;
    }

    // 生成随机字符串
    public static String getRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(HEX_CHARS.charAt(random.nextInt(HEX_CHARS.length())));
        }
        return sb.toString();
    }
}
