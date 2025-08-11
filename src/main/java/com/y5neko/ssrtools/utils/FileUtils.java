package com.y5neko.ssrtools.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.io.File;

/**
 * 文件工具类
 */
public class FileUtils {
    /**
     * 读取文件为字符串
     * @param filePath 文件路径
     * @return 字符串
     */
    public static String readFile(String filePath) {
        StringBuilder content = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        Files.newInputStream(Paths.get(filePath)), StandardCharsets.UTF_8))) {

            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {
                if (!firstLine) {
                    content.append(System.lineSeparator());
                } else {
                    firstLine = false;
                }
                content.append(line);
            }

        } catch (IOException e) {
            LogUtils.error(FileUtils.class, "读取文件失败：" + e.getMessage());
        }

        return content.toString();
    }

    /**
     * 复制文件夹（包含所有子目录和文件）
     * @param sourcePath 源文件夹路径
     * @param targetPath 目标文件夹路径
     * @throws IOException 如果复制过程中出错
     */
    public static void copyFolder(String sourcePath, String targetPath) throws IOException {
        Path source = Paths.get(sourcePath);
        Path target = Paths.get(targetPath);

        // 如果目标目录不存在，则创建
        if (!Files.exists(target)) {
            Files.createDirectories(target);
        }

        // 遍历源文件夹
        Files.walk(source)
                .forEach(src -> {
                    try {
                        Path dest = target.resolve(source.relativize(src));
                        if (Files.isDirectory(src)) {
                            Files.createDirectories(dest); // 创建子目录
                        } else {
                            Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING); // 复制文件
                        }
                    } catch (IOException e) {
                        LogUtils.error(FileUtils.class, "复制文件失败：" + e.getMessage());
                    }
                });
    }

    // 清空文件夹
    public static void cleanDirectory(String dirPath) throws IOException {
        org.apache.commons.io.FileUtils.cleanDirectory(new File(dirPath));
    }

    /**
     * 安全覆盖文件内容
     * @param filePath 文件路径
     * @param content 新内容
     * @param charset 字符集（如StandardCharsets.UTF_8）
     */
    public static void overwrite(String filePath, String content, Charset charset)
            throws IOException {
        Path path = Paths.get(filePath);

        // 检查父目录是否存在
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        // 通过临时文件原子操作
        Path tempFile = null;
        if (parent != null) {
            tempFile = Files.createTempFile(parent, "temp", ".tmp");
        }
        try {
            if (tempFile != null) {
                Files.write(tempFile, content.getBytes(charset));
            }
            if (tempFile != null) {
                Files.move(tempFile, path,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            }
        } catch (IOException e) {
            Files.deleteIfExists(tempFile); // 清理临时文件
            LogUtils.error(FileUtils.class, "覆盖文件失败：" + e.getMessage());
        }
    }
}
