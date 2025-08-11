package com.y5neko.ssrtools.utils;

import com.y5neko.ssrtools.GUI;

import java.io.File;
import java.security.SecureRandom;

/**
 * 杂项工具类
 */
public class MiscUtils {
    private static final String HEX_CHARS = "0123456789ABCDEF";
    private static final String NUM_CHARS = "0123456789";
    private static final SecureRandom random = new SecureRandom();

    // 通过参数传入目录数组初始化程序所需目录
    public static void initDir(String[] dirs) {
        for (String dir : dirs) {
            File file = new File(MiscUtils.getAbsolutePath(dir));
            if (!file.exists()) {
                file.mkdirs();
            }
        }
    }

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

    /**
     * 获取基于 jar 包的相对路径的绝对路径(仅相对路径可用，绝对路径会报错)
     * @param relativePath 基于 jar 包的相对路径
     * @return 绝对路径
     */
    public static String getAbsolutePath(String relativePath) {
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }

        try {
            String path = GUI.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();

            File location = new File(path);

            String baseDir;
            if (location.isFile()) {
                // 生产环境
                baseDir = location.getParentFile().getAbsolutePath();
            } else {
                // 开发环境
                baseDir = System.getProperty("user.dir");
            }

            return new File(baseDir, relativePath).getAbsolutePath();
        } catch (Exception e) {
            LogUtils.error(MiscUtils.class, "获取绝对路径失败：" + e.getMessage());
            return "路径读取失败: " + e.getMessage();
        }
    }

    public static void main(String[] args) {
        System.out.println(getAbsolutePath("docTemplates"));
    }
}
