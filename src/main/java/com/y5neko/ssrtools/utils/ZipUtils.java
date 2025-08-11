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