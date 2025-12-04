package com.y5neko.ssrtools.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Word XML模板占位符提取工具
 * 用于从document.xml中提取包含特定占位符的完整w:p段落
 */
public class WordXmlPlaceholderExtractor {

    // 占位符类型映射
    private static final Map<String, String> PLACEHOLDER_TYPES = new HashMap<>();
    static {
        PLACEHOLDER_TYPES.put("first_heading_text", "first_level_heading.txt");
        PLACEHOLDER_TYPES.put("second_heading_text", "second_level_heading.txt");
        PLACEHOLDER_TYPES.put("third_heading_text", "third_level_heading.txt");
        PLACEHOLDER_TYPES.put("fourth_heading_text", "fourth_level_heading.txt");
        PLACEHOLDER_TYPES.put("normal_text", "normal_text.txt");
    }

    // 匹配完整w:p段落的正则表达式
    private static final String W_P_PATTERN = "<w:p[^>]*>.*?</w:p>";
    // 匹配占位符的正则表达式
    private static final String PLACEHOLDER_PATTERN = "\\{\\{\\{\\{\\{([^}]+)\\}\\}\\}\\}\\}";
    // 匹配PAGEREF _Toc的正则表达式（目录条目）
    private static final String PAGEREF_TOC_PATTERN = "PAGEREF\\s+_Toc[^\\s]*";

    /**
     * 从document.xml中提取包含占位符的w:p段落
     *
     * @param xmlFilePath document.xml文件路径
     * @param outputDir 输出目录路径
     * @throws IOException 文件读写异常
     */
    public static void extractPlaceholders(String xmlFilePath, String outputDir) throws IOException {
        extractPlaceholdersAndClean(xmlFilePath, outputDir, false);
    }

    /**
     * 从document.xml中提取包含占位符的w:p段落，并可选择是否从源文件中删除对应内容
     *
     * @param xmlFilePath document.xml文件路径
     * @param outputDir 输出目录路径
     * @param cleanSource 是否从源文件中删除提取的段落
     * @throws IOException 文件读写异常
     */
    public static void extractPlaceholdersAndClean(String xmlFilePath, String outputDir, boolean cleanSource) throws IOException {
        System.out.println("开始从 " + xmlFilePath + " 提取占位符段落...");
        if (cleanSource) {
            System.out.println("[清理] 启用源文件清理模式，提取后将从document.xml中删除对应段落");
        }

        // 读取XML文件内容
        String xmlContent = readFile(xmlFilePath);

        // 创建输出目录
        createOutputDirectory(outputDir);

        // 匹配所有w:p段落
        Pattern wPPattern = Pattern.compile(W_P_PATTERN, Pattern.DOTALL);
        Matcher wPMatcher = wPPattern.matcher(xmlContent);

        Pattern placeholderPattern = Pattern.compile(PLACEHOLDER_PATTERN);

        int foundCount = 0;
        String cleanedXmlContent = xmlContent;

        // 用于记录需要删除的段落内容
        java.util.List<String> paragraphsToRemove = new java.util.ArrayList<>();

        // 第一遍：查找并保存需要提取的段落
        while (wPMatcher.find()) {
            String wPContent = wPMatcher.group();

            // 检查是否包含占位符
            Matcher placeholderMatcher = placeholderPattern.matcher(wPContent);
            if (placeholderMatcher.find()) {
                String placeholderName = placeholderMatcher.group(1);

                // 检查是否是我们需要提取的占位符类型
                if (PLACEHOLDER_TYPES.containsKey(placeholderName)) {
                    // 检查是否为目录条目（包含PAGEREF _Toc），如果是则跳过
                    if (isTableOfContentsEntry(wPContent)) {
                        System.out.println("[警告] 跳过目录条目: " + placeholderName + " (包含PAGEREF _Toc)");
                        System.out.println("  段落内容: " + wPContent.substring(0, Math.min(150, wPContent.length())) + "...");
                        continue;
                    }

                    // 替换paraId和name属性
                    String processedContent = replaceDynamicAttributes(wPContent);

                    String fileName = PLACEHOLDER_TYPES.get(placeholderName);
                    String outputPath = outputDir + File.separator + fileName;

                    // 保存处理后的w:p段落到文件
                    saveToFile(processedContent, outputPath);

                    System.out.println("[成功] 提取成功: " + placeholderName + " -> " + fileName);
                    System.out.println("  原始段落: " + wPContent.substring(0, Math.min(100, wPContent.length())) + "...");
                    System.out.println("  处理后段落: " + processedContent.substring(0, Math.min(100, processedContent.length())) + "...");

                    // 如果启用清理模式，记录需要删除的段落
                    if (cleanSource) {
                        paragraphsToRemove.add(wPContent);
                    }

                    foundCount++;
                }
            }
        }

        // 如果启用清理模式，从源文件中删除已提取的段落
        if (cleanSource && !paragraphsToRemove.isEmpty()) {
            System.out.println("\n[清理] 开始清理document.xml中的已提取段落...");

            cleanedXmlContent = xmlContent;
            int removedCount = 0;

            for (String paragraph : paragraphsToRemove) {
                // 使用字符串替换删除段落
                String before = cleanedXmlContent;
                cleanedXmlContent = cleanedXmlContent.replace(paragraph, "");

                if (!before.equals(cleanedXmlContent)) {
                    removedCount++;
                    System.out.println("  [成功] 删除段落: " + paragraph.substring(0, Math.min(80, paragraph.length())) + "...");
                }
            }

            // 保存清理后的XML内容
            saveToFile(cleanedXmlContent, xmlFilePath);
            System.out.println("[成功] 源文件清理完成，删除了 " + removedCount + " 个段落");
        }

        System.out.println("\n提取完成！共提取了 " + foundCount + " 个占位符段落。");
        System.out.println("输出目录: " + outputDir);
        if (cleanSource) {
            System.out.println("[成功] 源文件已同步清理");
        }
    }

    /**
     * 替换w:p段落中的动态属性
     * 将具体的paraId和name值替换为占位符
     *
     * @param wPContent w:p段落内容
     * @return 替换后的内容
     */
    private static String replaceDynamicAttributes(String wPContent) {
        String processedContent = wPContent;

        // 替换paraId属性：将具体的paraId值替换为占位符
        // 匹配格式：paraId="具体的值" 或 paraId="具体的值" 属性
        processedContent = processedContent.replaceAll(
                "paraId\\s*=\\s*\"[^\"]+\"",
                "paraId=\"{{{{{paraId}}}}}\""
        );

        // 替换name属性：将_Toc开头的name值替换为占位符
        // 匹配格式：name="_Toc开头的值"
        processedContent = processedContent.replaceAll(
                "name\\s*=\\s*\"(_Toc[^\"]*)\"",
                "name=\"{{{{{TocName}}}}}\""
        );

        return processedContent;
    }

    /**
     * 检查w:p段落是否包含PAGEREF _Toc（目录条目）
     * 如果包含，则跳过不提取
     *
     * @param wPContent w:p段落内容
     * @return 是否为目录条目
     */
    private static boolean isTableOfContentsEntry(String wPContent) {
        Pattern pagerEFPattern = Pattern.compile(PAGEREF_TOC_PATTERN);
        Matcher matcher = pagerEFPattern.matcher(wPContent);
        return matcher.find();
    }

    /**
     * 读取文件内容
     *
     * @param filePath 文件路径
     * @return 文件内容
     * @throws IOException 文件读取异常
     */
    private static String readFile(String filePath) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(filePath));
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * 创建输出目录
     *
     * @param outputDir 输出目录路径
     * @throws IOException 目录创建异常
     */
    private static void createOutputDirectory(String outputDir) throws IOException {
        File dir = new File(outputDir);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                System.out.println("[成功] 创建输出目录: " + outputDir);
            } else {
                throw new IOException("无法创建输出目录: " + outputDir);
            }
        } else {
            System.out.println("[成功] 使用已存在的输出目录: " + outputDir);
        }
    }

    /**
     * 保存内容到文件
     *
     * @param content 要保存的内容
     * @param filePath 文件路径
     * @throws IOException 文件写入异常
     */
    private static void saveToFile(String content, String filePath) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(filePath), StandardCharsets.UTF_8)) {
            writer.write(content);
            writer.write("\n"); // 添加换行符
        }
    }

    /**
     * 测试方法 - 使用示例
     */
    public static void main(String[] args) {
        try {
            // 测试文件路径和输出目录
            String testXmlPath = "test_document_with_attributes.xml";
            String outputDir = "extracted_components";

            // 检查测试文件是否存在
            File testFile = new File(testXmlPath);
            if (!testFile.exists()) {
                System.err.println("测试文件不存在: " + testXmlPath);
                System.err.println("请确保路径正确或使用实际存在的document.xml文件进行测试");

                // 提供使用说明
                System.out.println("\n使用方法:");
                System.out.println("1. 将此类集成到项目中");
                System.out.println("2. 调用 extractPlaceholders(xmlFilePath, outputDir) 方法");
                System.out.println("3. xmlFilePath: document.xml文件的完整路径");
                System.out.println("4. outputDir: 提取结果的输出目录");

                return;
            }

            // 执行提取
            extractPlaceholders(testXmlPath, outputDir);

        } catch (IOException e) {
            System.err.println("提取过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取支持的占位符类型列表
     *
     * @return 占位符类型映射
     */
    public static Map<String, String> getSupportedPlaceholders() {
        return new HashMap<>(PLACEHOLDER_TYPES);
    }

    /**
     * 验证文件是否为有效的document.xml
     *
     * @param filePath 文件路径
     * @return 是否有效
     */
    public static boolean isValidDocumentXml(String filePath) {
        try {
            String content = readFile(filePath);
            return content.contains("<w:document") && content.contains("</w:document>");
        } catch (IOException e) {
            return false;
        }
    }
}