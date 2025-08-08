package com.y5neko.ssrtools.utils;

import com.y5neko.ssrtools.models.docdata.ReportData;
import com.y5neko.ssrtools.models.docdata.SystemInfo;
import com.y5neko.ssrtools.models.docdata.Unit;
import com.y5neko.ssrtools.models.docdata.Vulnerability;
import com.y5neko.ssrtools.object.DocObj;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static com.y5neko.ssrtools.config.GlobalConfig.*;

/**
 * 文档生成工具类
 */
public class DocUtils {
    /**
     * 专用于正文处理多行内容
     * @param template 模板
     * @param lineText 每行文本
     * @return 处理后的行
     */
    private static String processNormalTextLine(String template, String lineText) {
        return template
                .replaceAll("\\{\\{\\{\\{\\{normal_text}}}}}", lineText)
                .replaceAll("\\{\\{\\{\\{\\{paraId}}}}}", MiscUtils.getParaID())
                .replaceAll("\\{\\{\\{\\{\\{TocName}}}}}", MiscUtils.getTocName());
    }

    /**
     * 替换占位符生成一级标题的原始内容
     * @param text 一级标题文本
     * @return 一级标题的原始内容
     */
    public static String firstHeadingGen(String text) {
        String contentTemplate = FileUtils.readFile(FIRST_LEVEL_HEADING_TEPMLATE_PATH);
        return contentTemplate
                .replaceAll("\\{\\{\\{\\{\\{first_heading_text}}}}}",text)
                .replaceAll("\\{\\{\\{\\{\\{paraId}}}}}", MiscUtils.getParaID())
                .replaceAll("\\{\\{\\{\\{\\{TocName}}}}}", MiscUtils.getTocName());
    }

    /**
     * 替换占位符生成二级标题的原始内容
     * @param text 二级标题文本
     * @return 二级标题的原始内容
     */
    public static String secondHeadingGen(String text) {
        String contentTemplate = FileUtils.readFile(SECOND_LEVEL_HEADING_TEPMLATE_PATH);
        return contentTemplate
                .replaceAll("\\{\\{\\{\\{\\{second_heading_text}}}}}",text)
                .replaceAll("\\{\\{\\{\\{\\{paraId}}}}}", MiscUtils.getParaID())
                .replaceAll("\\{\\{\\{\\{\\{TocName}}}}}", MiscUtils.getTocName());
    }

    /**
     * 替换占位符生成三级标题的原始内容
     * @param text 三级标题文本
     * @return 三级标题的原始内容
     */
    public static String thirdHeadingGen(String text) {
        String contentTemplate = FileUtils.readFile(THIRD_LEVEL_HEADING_TEPMLATE_PATH);
        return contentTemplate
                .replaceAll("\\{\\{\\{\\{\\{third_heading_text}}}}}",text)
                .replaceAll("\\{\\{\\{\\{\\{paraId}}}}}", MiscUtils.getParaID())
                .replaceAll("\\{\\{\\{\\{\\{TocName}}}}}", MiscUtils.getTocName());
    }

    /**
     * 替换占位符生成四级标题的原始内容
     * @param text 四级标题文本
     * @return 四级标题的原始内容
     */
    public static String fourthHeadingGen(String text) {
        String contentTemplate = FileUtils.readFile(FOURTH_LEVEL_HEADING_TEPMLATE_PATH);
        return contentTemplate
                .replaceAll("\\{\\{\\{\\{\\{fourth_heading_text}}}}}",text)
                .replaceAll("\\{\\{\\{\\{\\{paraId}}}}}", MiscUtils.getParaID())
                .replaceAll("\\{\\{\\{\\{\\{TocName}}}}}", MiscUtils.getTocName());
    }

    /**
     * 替换占位符生成正文的原始内容
     * @param text 正文文本
     * @return 正文文本的原始内容
     */
    public static String normalTextGen(String text) {
        String contentTemplate = FileUtils.readFile(NORMAL_TEXT_TEPMLATE_PATH);

        // 检测是否存在多行
        if (text.contains("\n")) {
            StringBuilder result = new StringBuilder();
            String[] lines = text.split("\n");

            for (int i = 0; i < lines.length; i++) {
                if (i > 0) result.append("\n");
                result.append(processNormalTextLine(contentTemplate, lines[i]));
            }

            return result.toString();
        } else {
            return processNormalTextLine(contentTemplate, text);
        }
    }

    /**
     * 替换占位符生成报告模板原始内容
     * @param docObj 报告模板信息对象
     * @return 报告模板原始内容
     */
    public static String contentGen(DocObj docObj) {
        String contentTemplates = FileUtils.readFile(DOC_TEMPLATE_PATH +"/word/document.xml");

        return contentTemplates
                .replaceAll("\\{\\{\\{\\{\\{customer_name}}}}}", docObj.getCustomerName())
                .replaceAll("\\{\\{\\{\\{\\{is_firsr_test}}}}}", docObj.getIsFirsrTest())
                .replaceAll("\\{\\{\\{\\{\\{signature_name}}}}}", docObj.getSignatureName())
                .replaceAll("\\{\\{\\{\\{\\{report_year}}}}}", Integer.toString(docObj.getReportYear()))
                .replaceAll("\\{\\{\\{\\{\\{report_month}}}}}", Integer.toString(docObj.getReportMonth()))
                .replaceAll("\\{\\{\\{\\{\\{report_day}}}}}", Integer.toString(docObj.getReportDay()))
                .replaceAll("\\{\\{\\{\\{\\{report_reporter}}}}}", docObj.getReporter())
                .replaceAll("\\{\\{\\{\\{\\{test_time}}}}}", docObj.getTestTime())
                .replaceAll("\\{\\{\\{\\{\\{vul_all_count}}}}}", Integer.toString(docObj.getVulAllCount()))
                .replaceAll("\\{\\{\\{\\{\\{vul_high_count}}}}}", Integer.toString(docObj.getVulHighCount()))
                .replaceAll("\\{\\{\\{\\{\\{vul_medium_count}}}}}", Integer.toString(docObj.getVulMediumCount()))
                .replaceAll("\\{\\{\\{\\{\\{vul_low_count}}}}}", Integer.toString(docObj.getVulLowCount()))
                .replaceAll("\\{\\{\\{\\{\\{pm_name}}}}}", docObj.getPmName())
                .replaceAll("\\{\\{\\{\\{\\{tester_name}}}}}", docObj.getTesterName());
    }

    /**
     * 替换占位符生成报告漏洞相关原始内容
     * @param reportData 报告数据
     * @param docObj 报告对象
     * @return 报告漏洞相关原始内容
     */
    public static String mainContentGen(ReportData reportData, DocObj docObj) {
        StringBuilder mainContent = new StringBuilder();

        // 遍历单位
        for (Unit unit : reportData.getUnits()) {
            mainContent.append(DocUtils.firstHeadingGen(unit.getUnitName()));

            // 遍历系统
            for (SystemInfo systemInfo : unit.getSystems()) {
                mainContent.append(DocUtils.secondHeadingGen(systemInfo.getSystemName()));

                // 遍历漏洞
                for (Vulnerability vulnerability : systemInfo.getVulnerabilities()) {
                    if (docObj.getIsFirsrTest().equals("初测")) {
                        mainContent.append(DocUtils.thirdHeadingGen(
                                "【" + vulnerability.getRiskLevel() + "】"
                                        + vulnerability.getName()
                        ));
                    } else {
                        mainContent.append(DocUtils.thirdHeadingGen(
                                "【" + vulnerability.getRiskLevel() + "】" +
                                        vulnerability.getName() +
                                        "（" + vulnerability.getIsFixed() + "）"
                        ));
                    }

                    // 漏洞描述
                    mainContent.append(DocUtils.fourthHeadingGen("漏洞描述"));
                    mainContent.append(DocUtils.normalTextGen(vulnerability.getVulDesc()));
                    // 风险等级
                    mainContent.append(DocUtils.fourthHeadingGen("风险等级"));
                    mainContent.append(DocUtils.normalTextGen(vulnerability.getRiskLevel()));
                    // 漏洞危害
                    mainContent.append(DocUtils.fourthHeadingGen("漏洞危害"));
                    mainContent.append(DocUtils.normalTextGen(vulnerability.getVulHazards()));
                    // 漏洞链接地址
                    mainContent.append(DocUtils.fourthHeadingGen("漏洞链接地址"));
                    mainContent.append(DocUtils.normalTextGen(vulnerability.getVulLinks()));
                    // 漏洞证明
                    mainContent.append(DocUtils.fourthHeadingGen("漏洞证明"));
                    mainContent.append(DocUtils.normalTextGen(vulnerability.getVulDetail()));
                    // 修复建议
                    mainContent.append(DocUtils.fourthHeadingGen("修复建议"));
                    mainContent.append(DocUtils.normalTextGen(vulnerability.getVulFixSuggestion()));
                }
            }
        }
        return mainContent.toString();
    }

    /**
     * 生成最终文档
     * @param docTemplatesPath 文档模板路径
     * @param docContent 文档主要内容
     * @throws IOException IOE
     * @param docObj 文档对象
     * @return 文档路径
     */
    public static String docGen(String docTemplatesPath, String docContent, DocObj docObj) throws IOException {
        String path = DOC_TEMPLATE_PATH;
        if (docTemplatesPath != null) {
            path = docTemplatesPath;
        }

        // 清空缓存目录
        FileUtils.cleanDirectory(TEMP_DIR);

        // 复制到缓存目录
        FileUtils.copyFolder(path, TEMP_DIR + "/doc");

        // 替换document.xml
        FileUtils.overwrite(TEMP_DIR + "/doc/word/document.xml", docContent, StandardCharsets.UTF_8);

        // 生成文档
        List<String> sources = Arrays.asList(
                TEMP_DIR + "/doc/_rels",
                TEMP_DIR + "/doc/customXml",
                TEMP_DIR + "/doc/docProps",
                TEMP_DIR + "/doc/word",
                TEMP_DIR + "/doc/[Content_Types].xml"
        );

        String reportPath = DOC_OUTPUT_DIR + File.separator +
                docObj.getCustomerName() +
                ("复测".equals(docObj.getIsFirsrTest()) ? "渗透测试复测报告" : "渗透测试报告") +
                docObj.getReportYear() +
                String.format("%02d", docObj.getReportMonth()) +
                String.format("%02d", docObj.getReportDay()) + "_" +
                MiscUtils.getRandomString(4) +
                ".docx";

        ZipUtils.zipMultiple(sources, reportPath);

        // 再次清空
        FileUtils.cleanDirectory(TEMP_DIR);

        return System.getProperty("user.dir") + File.separator + reportPath;
    }
}
