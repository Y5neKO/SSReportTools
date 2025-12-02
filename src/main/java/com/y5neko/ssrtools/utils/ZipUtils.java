package com.y5neko.ssrtools.utils;

import java.io.*;
import java.nio.file.*;
import java.util.zip.*;
import java.util.*;

import static com.y5neko.ssrtools.config.GlobalConfig.DOC_TEMPLATE_PATH;

/**
 * 压缩工具类
 */
public class ZipUtils {
    /**
     * 压缩文件或文件夹为ZIP
     * @param sourcePath 要压缩的文件/文件夹路径
     * @param outputPath 输出的ZIP文件路径
     * @throws IOException 如果发生IO错误
     */
    public static void zip(String sourcePath, String outputPath) throws IOException {
        Path source = Paths.get(sourcePath);
        if (!Files.exists(source)) {
            throw new FileNotFoundException("源路径不存在: " + sourcePath);
        }

        try (ZipOutputStream zos = new ZipOutputStream(
                Files.newOutputStream(Paths.get(outputPath)))) {

            if (Files.isDirectory(source)) {
                // 压缩文件夹
                Files.walk(source)
                        .filter(path -> !Files.isDirectory(path))
                        .forEach(path -> {
                            String entryName = source.relativize(path).toString()
                                    .replace(File.separator, "/");
                            addToZip(zos, path, entryName);
                        });
            } else {
                // 压缩单个文件
                addToZip(zos, source, source.getFileName().toString());
            }
        }
    }

    /**
     * 添加到压缩包
     * @param zos zip输出流
     * @param filePath 文件路径
     * @param entryName entry名称
     */
    private static void addToZip(ZipOutputStream zos, Path filePath, String entryName) {
        try (InputStream fis = Files.newInputStream(filePath)) {
            ZipEntry zipEntry = new ZipEntry(entryName);
            zos.putNextEntry(zipEntry);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) >= 0) {
                zos.write(buffer, 0, length);
            }
            zos.closeEntry();
        } catch (IOException e) {
            LogUtils.error(ZipUtils.class, "添加文件到ZIP失败：" + e.getMessage());
        }
    }

    /**
     * 压缩多个文件/文件夹到同一个ZIP
     * @param sources 要压缩的路径列表
     * @param outputPath 输出的ZIP文件路径
     */
    public static void zipMultiple(List<String> sources, String outputPath) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(
                Files.newOutputStream(Paths.get(outputPath)))) {

            for (String sourcePath : sources) {
                Path source = Paths.get(sourcePath);
                if (!Files.exists(source)) {
                    throw new FileNotFoundException("源路径不存在: " + sourcePath);
                }

                if (Files.isDirectory(source)) {
                    Files.walk(source)
                            .filter(path -> !Files.isDirectory(path))
                            .forEach(path -> {
                                String entryName = source.getFileName() + "/" +
                                        source.relativize(path).toString()
                                                .replace(File.separator, "/");
                                addToZip(zos, path, entryName);
                            });
                } else {
                    addToZip(zos, source, source.getFileName().toString());
                }
            }
        }
    }

    /**
     * 解压ZIP文件到指定目录
     * @param zipPath ZIP文件路径
     * @param destPath 解压目标目录
     * @throws IOException 如果发生IO错误
     */
    public static void extractZip(String zipPath, String destPath) throws IOException {
        File destDir = new File(destPath);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        try (ZipInputStream zis = new ZipInputStream(
                Files.newInputStream(Paths.get(zipPath)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                File entryFile = new File(destDir, entryName);

                // 确保解压路径不跳出目标目录（安全检查）
                if (!entryFile.getCanonicalPath().startsWith(destDir.getCanonicalPath())) {
                    throw new IOException("非法的ZIP条目路径: " + entryName);
                }

                if (entry.isDirectory()) {
                    // 创建目录
                    entryFile.mkdirs();
                } else {
                    // 创建父目录
                    entryFile.getParentFile().mkdirs();

                    // 解压文件
                    try (FileOutputStream fos = new FileOutputStream(entryFile)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

  public static void main(String[] args) {
        List<String> sources = Arrays.asList(
                MiscUtils.getAbsolutePath(DOC_TEMPLATE_PATH) + "/_rels",
                MiscUtils.getAbsolutePath(DOC_TEMPLATE_PATH) + "/customXml",
                MiscUtils.getAbsolutePath(DOC_TEMPLATE_PATH) + "/docProps",
                MiscUtils.getAbsolutePath(DOC_TEMPLATE_PATH) + "/word",
                MiscUtils.getAbsolutePath(DOC_TEMPLATE_PATH) + "/[Content_Types].xml"
        );

        try {
            ZipUtils.zipMultiple(sources, "sss.docx");
            System.out.println("多文件压缩完成");
        } catch (IOException e) {
            LogUtils.error(ZipUtils.class, "压缩失败：" + e.getMessage());
        }
    }
}