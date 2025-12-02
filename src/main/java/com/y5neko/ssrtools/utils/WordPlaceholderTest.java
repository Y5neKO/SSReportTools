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

        // 1. 先正常修复分割占位符
        String fixedXml = fixFragmentedPlaceholdersInOriginalXml(documentXml);
        System.out.println("修复分割占位符完成");

        // 2. 单独处理MainContent：提取所有<w:r></w:r>，检测到MainContent相关，直接替换
        fixedXml = processMainContentPlaceholderOptimized(fixedXml);
        System.out.println("处理MainContent占位符完成");

        // 统计修复结果
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
     * 直接在原始XML上修复分割的占位符，保留所有样式元素
     */
    private static String fixFragmentedPlaceholdersInOriginalXml(String xml) {
        System.out.println("\n=== 开始修复分割占位符（保留原始样式） ===");

        String result = xml;
        int fixCount = 0;

        // 1. 提取所有的<w:r>内容，按顺序排列
        List<WRElement> wrElements = extractWRElements(xml);
        System.out.println("提取到 " + wrElements.size() + " 个<w:r>元素");

        // 2. 识别被切割的占位符段
        List<PlaceholderSegment> placeholderSegments = identifyFragmentedPlaceholders(wrElements);
        System.out.println("识别到 " + placeholderSegments.size() + " 个分割占位符段");

        // 3. 按索引倒序处理，避免位置偏移问题
        List<PlaceholderSegment> sortedSegments = new ArrayList<>(placeholderSegments);
        sortedSegments.sort((a, b) -> Integer.compare(b.startIndex, a.startIndex));

        // 4. 逐个修复分割的占位符
        for (PlaceholderSegment segment : sortedSegments) {
            System.out.println("\n处理分割占位符段: " + segment.startIndex + " -> " + segment.endIndex);

            // 5. 提取完整占位符
            String fullPlaceholder = extractFullPlaceholderFromSegment(wrElements, segment);
            System.out.println("  提取的完整占位符: [" + fullPlaceholder + "]");

            if (fullPlaceholder != null && isValidPlaceholder(fullPlaceholder)) {
                // 6. 检查是否是MainContent占位符，进行特殊处理
                if (fullPlaceholder.contains("MainContent")) {
                    System.out.println("  -> 检测到MainContent占位符，进行特殊处理");

                    // 对MainContent占位符，直接输出纯占位符，不保留<w:r>标签
                    String replacement = fullPlaceholder; // 直接使用占位符本身

                    // 构建原始分割段（包括中间的样式元素）
                    String originalSegment = buildOriginalSegmentWithNonTextElements(wrElements, segment);

                    // 替换原始XML中的对应段
                    result = result.replace(originalSegment, replacement);
                    fixCount++;

                    System.out.println("  -> MainContent占位符已替换为纯文本，便于后续段落替换");
                } else {
                    // 7. 构建修复后的单个<w:r>元素，保留原始属性和样式
                    WRElement firstElement = wrElements.get(segment.startIndex);
                    String fixedSegment = buildFixedWRElementWithPlaceholder(firstElement.content, fullPlaceholder);

                    // 8. 构建原始分割段（包括中间的样式元素）
                    String originalSegment = buildOriginalSegmentWithNonTextElements(wrElements, segment);

                    // 9. 保留分割段中的非文本元素（如换行符等）
                    List<WRElement> nonTextElements = extractNonTextElementsBetween(wrElements, segment);
                    String replacement = buildReplacementWithFixedPlaceholderAndNonTextElements(fixedSegment, nonTextElements);

                    // 替换原始XML中的对应段
                    result = result.replace(originalSegment, replacement);
                    fixCount++;

                    System.out.println("  -> 修复完成，保留了 " + nonTextElements.size() + " 个非文本元素");
                }
            } else {
                System.out.println("  -> 跳过（无法提取有效占位符）");
            }
        }

        System.out.println("\n总共修复了 " + fixCount + " 个分割占位符");
        System.out.println("=== 分割占位符修复完成 ===");
        return result;
    }

    /**
     * 提取分割段中非文本元素（如换行符、制表符等）
     */
    private static List<WRElement> extractNonTextElementsBetween(List<WRElement> elements, PlaceholderSegment segment) {
        List<WRElement> nonTextElements = new ArrayList<>();

        for (int i = segment.startIndex + 1; i < segment.endIndex; i++) {
            WRElement element = elements.get(i);
            // 检查是否是不包含文本的样式元素
            if (!element.content.contains("<w:t>") || !element.content.contains("</w:t>")) {
                nonTextElements.add(element);
            }
        }

        return nonTextElements;
    }

    /**
     * 构建包含修复占位符和非文本元素的替换内容
     */
    private static String buildReplacementWithFixedPlaceholderAndNonTextElements(String fixedPlaceholderElement, List<WRElement> nonTextElements) {
        StringBuilder result = new StringBuilder(fixedPlaceholderElement);

        // 在修复后的占位符元素后添加非文本元素
        for (WRElement nonTextElement : nonTextElements) {
            result.append(nonTextElement.content);
        }

        return result.toString();
    }

    /**
     * 构建原始分割段的内容（包括中间的所有元素）
     */
    private static String buildOriginalSegmentWithNonTextElements(List<WRElement> elements, PlaceholderSegment segment) {
        StringBuilder original = new StringBuilder();
        for (int i = segment.startIndex; i <= segment.endIndex; i++) {
            original.append(elements.get(i).content);
        }
        return original.toString();
    }

    /**
     * 修复被分割的占位符 - 核心算法（已弃用，保留作参考）
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
     * 将修复后的占位符应用到原始XML中，保留原始样式
     */
    private static String applyFixedPlaceholdersToOriginalXml(String originalXml, String fixedXml) {
        System.out.println("\n=== 开始将修复结果应用到原始XML ===");

        // 1. 从原始XML中提取所有<w:r>元素
        List<WRElement> originalElements = extractWRElements(originalXml);

        // 2. 从修复后的XML中提取所有占位符
        List<String> fixedPlaceholders = extractAllPlaceholders(fixedXml);

        System.out.println("原始XML有 " + originalElements.size() + " 个<w:r>元素");
        System.out.println("修复后有 " + fixedPlaceholders.size() + " 个完整占位符");

        // 3. 在原始XML中找到并替换对应的分割占位符段
        String result = originalXml;
        int replacedCount = 0;

        // 重新识别原始XML中的分割占位符
        List<PlaceholderSegment> originalSegments = identifyFragmentedPlaceholders(originalElements);

        for (int i = 0; i < originalSegments.size() && i < fixedPlaceholders.size(); i++) {
            PlaceholderSegment segment = originalSegments.get(i);
            String fixedPlaceholder = fixedPlaceholders.get(i);

            // 构建原始分割段
            String originalSegment = buildOriginalSegment(originalElements, segment);

            // 获取第一个元素的<w:r>属性
            WRElement firstElement = originalElements.get(segment.startIndex);
            String fixedSegment = buildFixedWRElementWithPlaceholder(firstElement.content, fixedPlaceholder);

            // 替换
            result = result.replace(originalSegment, fixedSegment);
            replacedCount++;

            System.out.println("  替换段 " + segment.startIndex + "-" + segment.endIndex + ": " + fixedPlaceholder);
        }

        System.out.println("总共替换了 " + replacedCount + " 个占位符段");
        System.out.println("=== 应用修复结果完成 ===\n");

        return result;
    }

    /**
     * 从修复后的XML中提取所有完整占位符
     */
    private static List<String> extractAllPlaceholders(String xml) {
        List<String> placeholders = new ArrayList<>();
        Pattern pattern = Pattern.compile("<w:t[^>]*>(.*?)</w:t>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(xml);

        while (matcher.find()) {
            String text = matcher.group(1);
            if (text.contains(PLACEHOLDER_START) && text.contains(PLACEHOLDER_END)) {
                placeholders.add(text);
            }
        }

        return placeholders;
    }

    /**
     * 构建包含完整占位符的<w:r>元素
     */
    private static String buildFixedWRElementWithPlaceholder(String originalElementContent, String placeholder) {
        // 提取原始<w:r>的结构
        Pattern wrPattern = Pattern.compile("^(<w:r[^>]*>)(.*?)(<w:t[^>]*>)(.*?)(</w:t>)(.*?)(</w:r>)$", Pattern.DOTALL);
        Matcher wrMatcher = wrPattern.matcher(originalElementContent);

        if (wrMatcher.find()) {
            String wrStart = wrMatcher.group(1);     // <w:r>开始标签
            String beforeT = wrMatcher.group(2);     // <w:t>之前的内容（如<w:rPr>）
            String tStart = wrMatcher.group(3);      // <w:t>开始标签
            String tEnd = wrMatcher.group(5);        // </w:t>结束标签
            String afterT = wrMatcher.group(6);      // </w:t>之后的内容
            String wrEnd = wrMatcher.group(7);       // </w:r>结束标签

            // 构建新的内容，保留原始结构，只替换占位符
            return wrStart + beforeT + tStart + placeholder + tEnd + afterT + wrEnd;
        }

        return originalElementContent; // 如果解析失败，返回原内容
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

    /**
     * 优化处理MainContent占位符：先定位再提取段落
     */
    private static String processMainContentPlaceholderOptimized(String xml) {
        System.out.println("\n=== 开始优化处理MainContent占位符 ===");

        String result = xml;
        int processedCount = 0;

        // 循环处理所有可能的MainContent占位符（防止有多个）
        while (true) {
            // 1. 先找到MainContent占位符的位置
            int mainContentPos = result.indexOf("{{{{{MainContent}}}}}");
            if (mainContentPos == -1) {
                break; // 没有更多MainContent占位符了
            }

            System.out.println("  找到MainContent位置: " + mainContentPos);

            // 2. 向前查找最近的<w:p>开始位置，但要确保是正确的段落开始
            int pStart = findCorrectParagraphStart(result, mainContentPos);
            if (pStart == -1) {
                System.out.println("  未找到正确的<w:p>开始标签");
                break;
            }

            // 3. 向后查找最近的</w:p>结束位置，但要确保是正确的段落结束
            int pEnd = findCorrectParagraphEnd(result, mainContentPos);
            if (pEnd == -1) {
                System.out.println("  未找到正确的</w:p>结束标签");
                break;
            }
            pEnd += 6; // +6 是"</w:p>"的长度

            System.out.println("  段落范围: " + pStart + " - " + pEnd + ", 长度: " + (pEnd - pStart));

            // 调试：显示段落开始前后的内容
            System.out.println("  段落开始前50字符: " + result.substring(Math.max(0, pStart-50), pStart));
            System.out.println("  段落开始后50字符: " + result.substring(pStart, Math.min(result.length(), pStart+50)));
            System.out.println("  段落结束前10字符: " + result.substring(Math.max(0, pEnd-10), pEnd));
            System.out.println("  段落结束后10字符: " + result.substring(pEnd, Math.min(result.length(), pEnd+10)));

            // 4. 提取整个段落进行验证
            String originalParagraph = result.substring(pStart, pEnd);
            System.out.println("  段落内容长度: " + originalParagraph.length());
            System.out.println("  段落前100字符: " + originalParagraph.substring(0, Math.min(100, originalParagraph.length())));

            // 5. 验证这个段落确实包含MainContent占位符
            if (!originalParagraph.contains("{{{{{MainContent}}}}}")) {
                System.out.println("  错误：找到的段落不包含MainContent占位符，跳过");
                break;
            }

            // 6. 替换整个段落为纯占位符
            result = result.substring(0, pStart) + "{{{{{MainContent}}}}" + result.substring(pEnd);
            processedCount++;

            System.out.println("  替换MainContent段落完成");
        }

        System.out.println("总共处理了 " + processedCount + " 个MainContent段落");
        System.out.println("=== MainContent占位符优化处理完成 ===\n");

        return result;
    }

    /**
     * 查找正确的段落开始位置
     */
    private static int findCorrectParagraphStart(String content, int searchPos) {
        // 从MainContent位置向前搜索，找到所有<w:p>标签
        int searchIndex = searchPos;

        while (searchIndex >= 0) {
            int foundPos = content.lastIndexOf("<w:p", searchIndex);
            if (foundPos == -1) {
                break;
            }

            // 检查找到的标签是否是完整的段落开始标签（不是段落属性标签<w:pPr>）
            int tagEnd = content.indexOf(">", foundPos);
            if (tagEnd != -1) {
                String tagContent = content.substring(foundPos, tagEnd + 1);

                // 必须是<w:p>开始标签，不能是<w:pPr>
                if (tagContent.startsWith("<w:p") && !tagContent.startsWith("<w:pPr")) {
                    System.out.println("    候选段落开始位置: " + foundPos);
                    System.out.println("    候选段落开始标签: " + tagContent);

                    // 验证这个<w:p>到MainContent之间确实包含MainContent占位符
                    int endIndex = Math.min(searchPos + 200, content.length());
                    String segment = content.substring(foundPos, endIndex);

                    if (segment.contains("{{{{{MainContent}}}}}")) {
                        System.out.println("    段落开始验证通过");
                        return foundPos;
                    } else {
                        System.out.println("    警告：找到的<w:p>标签不包含MainContent");
                        System.out.println("    段落内容片段: " + segment);
                    }
                }
            }

            searchIndex = foundPos - 1;
        }

        return -1;
    }

    /**
     * 查找正确的段落结束位置
     */
    private static int findCorrectParagraphEnd(String content, int searchPos) {
        // 从MainContent位置向后搜索，找到最近的</w:p>标签
        int foundPos = content.indexOf("</w:p>", searchPos);
        if (foundPos == -1) {
            return -1;
        }

        // 验证这个</w:p>确实是MainContent所在段落的结束
        // 通过检查从段落开始到结束是否包含MainContent
        int paragraphStart = content.lastIndexOf("<w:p", searchPos);
        if (paragraphStart != -1) {
            String candidateParagraph = content.substring(paragraphStart, foundPos + 6);
            if (candidateParagraph.contains("{{{{{MainContent}}}}}")) {
                return foundPos;
            } else {
                System.out.println("    警告：找到的</w:p>标签不包含MainContent");
                return -1;
            }
        }

        return -1;
    }
}