package com.y5neko.ssrtools.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.*;
import java.util.*;

/**
 * Word占位符修复测试工具
 * 独立测试类，用于处理document.xml文件中的占位符修复
 */
public class WordPlaceholderTest {

    // 占位符模式
    private static final String PLACEHOLDER_START = "{{{{{";
    private static final String PLACEHOLDER_END = "}}}}}";

    /**
     * 读取document.xml文件
     */
    public static String readDocumentXml(String filePath) throws IOException {
        return FileUtils.readFile(filePath);
    }

    /**
     * 写入修复后的document.xml文件
     */
    public static void writeDocumentXml(String filePath, String content) throws IOException {
        FileUtils.overwrite(filePath, content, StandardCharsets.UTF_8);
    }

    /**
     * 修复document.xml中的占位符 - 主方法
     */
    public static String fixPlaceholders(String documentXml) {
        System.out.println("开始修复占位符...");

        // 1. 高级修复：移除干扰元素
        String cleanedXml = removeInterferingElements(documentXml);
        System.out.println("1. 移除干扰元素完成");

        // 2. 修复被分割的占位符
        String fixedXml = fixFragmentedPlaceholders(cleanedXml);
        System.out.println("2. 修复分割占位符完成");

        // 3. 统计修复结果
        statisticsRepair(documentXml, fixedXml);

        return fixedXml;
    }

    /**
     * 移除干扰元素（如书签等）
     */
    private static String removeInterferingElements(String xml) {
        // 移除书签标记
        xml = xml.replaceAll("<w:bookmarkStart[^>]*/>", "");
        xml = xml.replaceAll("<w:bookmarkEnd[^>]*/>", "");
        xml = xml.replaceAll("<w:bookmarkStart[^>]*>.*?</w:bookmarkStart>", "");

        // 移除其他干扰元素
        xml = xml.replaceAll("<w:proof[^>]*/>", "");
        xml = xml.replaceAll("<w:lastRenderedPageBreak[^>]*/>", "");
        xml = xml.replaceAll("<w:br[^>]*/>", "");
        xml = xml.replaceAll("<w:tab/>", "");

        return xml;
    }

    /**
     * 修复被分割的占位符 - 核心算法
     */
    private static String fixFragmentedPlaceholders(String xml) {
        System.out.println("\n=== 开始修复分割占位符 ===");

        String result = xml;
        int fixCount = 0;

        // 按照您的思路实现

        // 1. 提取所有的<w:r>内容，按顺序排列
        List<WRElement> wrElements = extractWRElements(xml);
        System.out.println("提取到 " + wrElements.size() + " 个<w:r>元素");

        // 2. 识别被切割的占位符段
        List<PlaceholderSegment> placeholderSegments = identifyFragmentedPlaceholders(wrElements);
        System.out.println("识别到 " + placeholderSegments.size() + " 个分割占位符段");

        // 3. 逐个修复分割的占位符
        for (PlaceholderSegment segment : placeholderSegments) {
            System.out.println("\n处理分割占位符段: " + segment.startIndex + " -> " + segment.endIndex);

            // 4. 记录第一组<w:r>的<w:r>到占位符开始之间的内容
            WRElement firstElement = wrElements.get(segment.startIndex);
            String firstContent = extractContentBeforePlaceholder(firstElement.content, PLACEHOLDER_START);
            System.out.println("  第一组开始前内容: [" + firstContent + "]");

            // 5. 识别占位符的所有相关文字
            String fullPlaceholder = extractFullPlaceholderFromSegment(wrElements, segment);
            System.out.println("  提取的完整占位符: [" + fullPlaceholder + "]");

            // 6. 记录最后一组<w:r>的占位符结束之后的内容
            WRElement lastElement = wrElements.get(segment.endIndex);
            String lastContent = extractContentAfterPlaceholder(lastElement.content, PLACEHOLDER_END);
            System.out.println("  最后一组结束后内容: [" + lastContent + "]");

            // 7. 拼接成正确的占位符段
            String fixedSegment = buildFixedWRElement(firstElement.content, firstContent, fullPlaceholder, lastContent);
            System.out.println("  修复后的完整段: " + fixedSegment);

            // 替换原始XML中的对应段
            String originalSegment = buildOriginalSegment(wrElements, segment);
            result = result.replace(originalSegment, fixedSegment);
            fixCount++;

            System.out.println("  -> 修复完成");
        }

        System.out.println("\n总共修复了 " + fixCount + " 个分割占位符");
        System.out.println("=== 分割占位符修复完成 ===");
        return result;
    }

    /**
     * 提取所有的<w:r>元素
     */
    private static List<WRElement> extractWRElements(String xml) {
        List<WRElement> elements = new ArrayList<>();
        Pattern wrPattern = Pattern.compile("<w:r[^>]*>.*?</w:r>", Pattern.DOTALL);
        Matcher matcher = wrPattern.matcher(xml);

        int index = 0;
        while (matcher.find()) {
            String content = matcher.group();
            elements.add(new WRElement(index, matcher.start(), matcher.end(), content));
            index++;
        }

        return elements;
    }

    /**
     * 识别被切割的占位符段
     */
    private static List<PlaceholderSegment> identifyFragmentedPlaceholders(List<WRElement> elements) {
        List<PlaceholderSegment> segments = new ArrayList<>();

        for (int i = 0; i < elements.size(); i++) {
            WRElement element = elements.get(i);
            String content = element.content;

            // 检查是否包含{{{{（但没有完整的占位符）
            if (content.contains(PLACEHOLDER_START) && !containsCompletePlaceholder(content)) {
                // 查找对应的结束标记
                for (int j = i + 1; j < elements.size(); j++) {
                    WRElement laterElement = elements.get(j);
                    if (laterElement.content.contains(PLACEHOLDER_END)) {
                        segments.add(new PlaceholderSegment(i, j));
                        break; // 找到配对的结束标记
                    }
                }
            }
        }

        return segments;
    }

    /**
     * 从内容中提取占位符开始前的内容
     */
    private static String extractContentBeforePlaceholder(String content, String placeholder) {
        int pos = content.indexOf(placeholder);
        if (pos == -1) return content;

        // 查找对应的<w:t>标签
        Pattern tPattern = Pattern.compile("^(.*?<w:t[^>]*>)(.*?)$", Pattern.DOTALL);
        Matcher tMatcher = tPattern.matcher(content.substring(0, pos));

        if (tMatcher.find()) {
            return tMatcher.group(1); // 返回<w:t>标签部分
        }
        return "";
    }

    /**
     * 从内容中提取占位符结束后的内容
     */
    private static String extractContentAfterPlaceholder(String content, String placeholder) {
        int pos = content.lastIndexOf(placeholder);
        if (pos == -1) return content;

        int afterPos = pos + placeholder.length();

        // 查找对应的</w:t>标签
        Pattern tPattern = Pattern.compile("^(.*?)(</w:t>.*?)$", Pattern.DOTALL);
        Matcher tMatcher = tPattern.matcher(content.substring(afterPos));

        if (tMatcher.find()) {
            return tMatcher.group(2); // 返回</w:t>标签部分
        }
        return "";
    }

    /**
     * 从分割的段中提取完整的占位符
     */
    private static String extractFullPlaceholderFromSegment(List<WRElement> elements, PlaceholderSegment segment) {
        StringBuilder placeholder = new StringBuilder();
        boolean inPlaceholder = false;

        for (int i = segment.startIndex; i <= segment.endIndex; i++) {
            String content = elements.get(i).content;

            // 提取<w:t>标签内的文本
            Pattern tPattern = Pattern.compile("<w:t[^>]*>(.*?)</w:t>", Pattern.DOTALL);
            Matcher tMatcher = tPattern.matcher(content);

            while (tMatcher.find()) {
                String text = tMatcher.group(1);
                placeholder.append(text);
            }
        }

        return placeholder.toString();
    }

    /**
     * 构建修复后的<w:r>元素
     */
    private static String buildFixedWRElement(String firstElementContent, String firstContent, String fullPlaceholder, String lastContent) {
        // 提取第一个<w:r>的属性部分
        Pattern wrPattern = Pattern.compile("^(<w:r[^>]*>)(.*?)(</w:r>)$", Pattern.DOTALL);
        Matcher wrMatcher = wrPattern.matcher(firstElementContent);

        if (wrMatcher.find()) {
            String wrStart = wrMatcher.group(1); // <w:r>开始标签
            String wrEnd = wrMatcher.group(3); // </w:r>结束标签

            // 构建新的内容：第一组开始前内容 + 完整占位符 + 最后一组结束后内容
            String newContent = firstContent + fullPlaceholder + lastContent;

            if (!newContent.startsWith(wrStart)) {
                newContent = wrStart + newContent;
            }

            if (!newContent.endsWith(wrEnd)) {
                newContent = newContent + wrEnd;
            }

            return newContent;
        }

        return firstElementContent; // 如果解析失败，返回原内容
    }

    /**
     * 构建原始分割段的内容
     */
    private static String buildOriginalSegment(List<WRElement> elements, PlaceholderSegment segment) {
        StringBuilder original = new StringBuilder();
        for (int i = segment.startIndex; i <= segment.endIndex; i++) {
            original.append(elements.get(i).content);
        }
        return original.toString();
    }

    /**
     * 检查内容是否包含完整的占位符
     */
    private static boolean containsCompletePlaceholder(String content) {
        return extractPlaceholder(content) != null;
    }

    /**
     * 内部类：WRElement
     */
    private static class WRElement {
        int index;
        int startPos;
        int endPos;
        String content;

        WRElement(int index, int startPos, int endPos, String content) {
            this.index = index;
            this.startPos = startPos;
            this.endPos = endPos;
            this.content = content;
        }
    }

    /**
     * 内部类：PlaceholderSegment
     */
    private static class PlaceholderSegment {
        int startIndex;
        int endIndex;

        PlaceholderSegment(int startIndex, int endIndex) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        @Override
        public String toString() {
            return "[" + startIndex + "-" + endIndex + "]";
        }
    }

    /**
     * 提取完整占位符
     */
    private static String extractPlaceholder(String text) {
        int startIndex = text.indexOf(PLACEHOLDER_START);
        if (startIndex == -1) return null;

        int endIndex = text.indexOf(PLACEHOLDER_END, startIndex);
        if (endIndex == -1) return null;

        String placeholder = text.substring(startIndex, endIndex + PLACEHOLDER_END.length());

        // 验证占位符格式
        if (isValidPlaceholder(placeholder)) {
            return placeholder;
        }

        return null;
    }

    /**
     * 验证占位符格式
     */
    private static boolean isValidPlaceholder(String placeholder) {
        if (!placeholder.startsWith(PLACEHOLDER_START) || !placeholder.endsWith(PLACEHOLDER_END)) {
            return false;
        }

        String innerContent = placeholder.substring(
            PLACEHOLDER_START.length(),
            placeholder.length() - PLACEHOLDER_END.length()
        );

        return !innerContent.trim().isEmpty() &&
               !innerContent.contains("{{{") &&
               !innerContent.contains("}}}");
    }

    /**
     * 重新构建XML，替换<w:t>标签内容
     */
    private static String rebuildXmlWithFixedTexts(String originalXml, List<String> fixedTexts) {
        System.out.println("\n=== 开始重建XML ===");
        // 由于新算法直接修改XML结构，这里直接返回修复后的XML
        System.out.println("=== XML重建完成 ===\n");
        return originalXml;
    }

    
    
    /**
     * 统计修复结果
     */
    private static void statisticsRepair(String originalXml, String fixedXml) {
        Pattern pattern = Pattern.compile("\\{\\{\\{\\{[^{}]*\\}\\}\\}\\}");

        Matcher originalMatcher = pattern.matcher(originalXml);
        Matcher fixedMatcher = pattern.matcher(fixedXml);

        int originalCount = 0;
        int fixedCount = 0;

        while (originalMatcher.find()) originalCount++;
        while (fixedMatcher.find()) fixedCount++;

        System.out.println("=== 修复统计 ===");
        System.out.println("原始完整占位符数量: " + originalCount);
        System.out.println("修复后完整占位符数量: " + fixedCount);
        System.out.println("新增修复的占位符: " + (fixedCount - originalCount));
        System.out.println("===============");
    }

    /**
     * 主测试方法
     */
    public static void main(String[] args) {
        // 在这里修改输入和输出文件路径
        String inputFile = "/Volumes/Y5Sec/Y5neKO/Project/Java_Project/SSReportTools/created_templates/模板制作测试/word/old_document.xml";           // 输入文件路径
        String outputFile = "/Volumes/Y5Sec/Y5neKO/Project/Java_Project/SSReportTools/created_templates/模板制作测试/word/document.xml";     // 输出文件路径

        try {
            System.out.println("读取文件: " + inputFile);
            String originalXml = readDocumentXml(inputFile);

            System.out.println("文件大小: " + originalXml.length() + " 字符");

            String fixedXml = fixPlaceholders(originalXml);

            System.out.println("写入修复后的文件: " + outputFile);
            writeDocumentXml(outputFile, fixedXml);

            System.out.println("修复完成！");

        } catch (IOException e) {
            System.err.println("处理文件时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
}