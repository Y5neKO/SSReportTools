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
import java.util.regex.Pattern;

import static com.y5neko.ssrtools.config.GlobalConfig.*;

public class DocUtils {

    /**
     * 统一安全替换（自动转义）
     */
    private static String safeReplace(String content, String pattern, String value) {
        return content.replaceAll(Pattern.quote(pattern), XmlEscapeUtils.escape(value));
    }

    /**
     * 专用于正文处理多行内容
     */
    private static String processNormalTextLine(String template, String lineText) {
        return template
                .replace("{{{{{normal_text}}}}}", XmlEscapeUtils.escape(lineText))
                .replace("{{{{{paraId}}}}}", MiscUtils.getParaID())
                .replace("{{{{{TocName}}}}}", MiscUtils.getTocName());
    }

    public static String firstHeadingGen(String text) {
        String contentTemplate = FileUtils.readFile(MiscUtils.getAbsolutePath(FIRST_LEVEL_HEADING_TEPMLATE_PATH));
        return contentTemplate
                .replace("{{{{{first_heading_text}}}}}", XmlEscapeUtils.escape(text))
                .replace("{{{{{paraId}}}}}", MiscUtils.getParaID())
                .replace("{{{{{TocName}}}}}", MiscUtils.getTocName());
    }

    public static String secondHeadingGen(String text) {
        String contentTemplate = FileUtils.readFile(MiscUtils.getAbsolutePath(SECOND_LEVEL_HEADING_TEPMLATE_PATH));
        return contentTemplate
                .replace("{{{{{second_heading_text}}}}}", XmlEscapeUtils.escape(text))
                .replace("{{{{{paraId}}}}}", MiscUtils.getParaID())
                .replace("{{{{{TocName}}}}}", MiscUtils.getTocName());
    }

    public static String thirdHeadingGen(String text) {
        String contentTemplate = FileUtils.readFile(MiscUtils.getAbsolutePath(THIRD_LEVEL_HEADING_TEPMLATE_PATH));
        return contentTemplate
                .replace("{{{{{third_heading_text}}}}}", XmlEscapeUtils.escape(text))
                .replace("{{{{{paraId}}}}}", MiscUtils.getParaID())
                .replace("{{{{{TocName}}}}}", MiscUtils.getTocName());
    }

    public static String fourthHeadingGen(String text) {
        String contentTemplate = FileUtils.readFile(MiscUtils.getAbsolutePath(FOURTH_LEVEL_HEADING_TEPMLATE_PATH));
        return contentTemplate
                .replace("{{{{{fourth_heading_text}}}}}", XmlEscapeUtils.escape(text))
                .replace("{{{{{paraId}}}}}", MiscUtils.getParaID())
                .replace("{{{{{TocName}}}}}", MiscUtils.getTocName());
    }

    public static String normalTextGen(String text) {
        String contentTemplate = FileUtils.readFile(MiscUtils.getAbsolutePath(NORMAL_TEXT_TEPMLATE_PATH));

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

    public static String contentGen(DocObj docObj) {

        String contentTemplates = FileUtils.readFile(
                MiscUtils.getAbsolutePath(DOC_TEMPLATE_PATH) + "/word/document.xml"
        );

        return contentTemplates
                .replace("{{{{{customer_name}}}}}", XmlEscapeUtils.escape(docObj.getCustomerName()))
                .replace("{{{{{is_firsr_test}}}}}", XmlEscapeUtils.escape(docObj.getIsFirsrTest()))
                .replace("{{{{{signature_name}}}}}", XmlEscapeUtils.escape(docObj.getSignatureName()))
                .replace("{{{{{report_year}}}}}", XmlEscapeUtils.escape(Integer.toString(docObj.getReportYear())))
                .replace("{{{{{report_month}}}}}", XmlEscapeUtils.escape(Integer.toString(docObj.getReportMonth())))
                .replace("{{{{{report_day}}}}}", XmlEscapeUtils.escape(Integer.toString(docObj.getReportDay())))
                .replace("{{{{{report_reporter}}}}}", XmlEscapeUtils.escape(docObj.getReporter()))
                .replace("{{{{{test_time}}}}}", XmlEscapeUtils.escape(docObj.getTestTime()))
                .replace("{{{{{vul_all_count}}}}}", XmlEscapeUtils.escape(Integer.toString(docObj.getVulAllCount())))
                .replace("{{{{{vul_high_count}}}}}", XmlEscapeUtils.escape(Integer.toString(docObj.getVulHighCount())))
                .replace("{{{{{vul_medium_count}}}}}", XmlEscapeUtils.escape(Integer.toString(docObj.getVulMediumCount())))
                .replace("{{{{{vul_low_count}}}}}", XmlEscapeUtils.escape(Integer.toString(docObj.getVulLowCount())))
                .replace("{{{{{pm_name}}}}}", XmlEscapeUtils.escape(docObj.getPmName()))
                .replace("{{{{{tester_name}}}}}", XmlEscapeUtils.escape(docObj.getTesterName()));
    }

    public static String mainContentGen(ReportData reportData, DocObj docObj) {
        StringBuilder mainContent = new StringBuilder();

        for (Unit unit : reportData.getUnits()) {
            mainContent.append(firstHeadingGen(unit.getUnitName()));

            for (SystemInfo systemInfo : unit.getSystems()) {
                mainContent.append(secondHeadingGen(systemInfo.getSystemName()));

                for (Vulnerability vulnerability : systemInfo.getVulnerabilities()) {

                    String title;
                    if (docObj.getIsFirsrTest().equals("初测")) {
                        title = "【" + vulnerability.getRiskLevel() + "】" + vulnerability.getName();
                    } else {
                        title = "【" + vulnerability.getRiskLevel() + "】" +
                                vulnerability.getName() + "（" + vulnerability.getIsFixed() + "）";
                    }
                    mainContent.append(thirdHeadingGen(title));

                    mainContent.append(fourthHeadingGen("漏洞描述"));
                    mainContent.append(normalTextGen(vulnerability.getVulDesc()));

                    mainContent.append(fourthHeadingGen("风险等级"));
                    mainContent.append(normalTextGen(vulnerability.getRiskLevel()));

                    mainContent.append(fourthHeadingGen("漏洞危害"));
                    mainContent.append(normalTextGen(vulnerability.getVulHazards()));

                    mainContent.append(fourthHeadingGen("漏洞链接地址"));
                    mainContent.append(normalTextGen(vulnerability.getVulLinks()));

                    mainContent.append(fourthHeadingGen("漏洞证明"));
                    mainContent.append(normalTextGen(vulnerability.getVulDetail()));

                    mainContent.append(fourthHeadingGen("修复建议"));
                    mainContent.append(normalTextGen(vulnerability.getVulFixSuggestion()));
                }
            }
        }
        return mainContent.toString();
    }

    public static String docGen(String docTemplatesPath, String docContent, DocObj docObj) throws IOException {

        String path = docTemplatesPath != null ? docTemplatesPath :
                MiscUtils.getAbsolutePath(DOC_TEMPLATE_PATH);

        FileUtils.cleanDirectory(MiscUtils.getAbsolutePath(TEMP_DIR));
        FileUtils.copyFolder(path, MiscUtils.getAbsolutePath(TEMP_DIR) + "/doc");

        FileUtils.overwrite(
                MiscUtils.getAbsolutePath(TEMP_DIR) + "/doc/word/document.xml",
                docContent,
                StandardCharsets.UTF_8
        );

        List<String> sources = Arrays.asList(
                MiscUtils.getAbsolutePath(TEMP_DIR) + "/doc/_rels",
                MiscUtils.getAbsolutePath(TEMP_DIR) + "/doc/customXml",
                MiscUtils.getAbsolutePath(TEMP_DIR) + "/doc/docProps",
                MiscUtils.getAbsolutePath(TEMP_DIR) + "/doc/word",
                MiscUtils.getAbsolutePath(TEMP_DIR) + "/doc/[Content_Types].xml"
        );

        String reportPath = MiscUtils.getAbsolutePath(DOC_OUTPUT_DIR) + File.separator +
                docObj.getCustomerName() +
                ("复测".equals(docObj.getIsFirsrTest()) ? "渗透测试复测报告" : "渗透测试报告") +
                docObj.getReportYear() +
                String.format("%02d", docObj.getReportMonth()) +
                String.format("%02d", docObj.getReportDay()) + "_" +
                MiscUtils.getRandomString(4) +
                ".docx";

        ZipUtils.zipMultiple(sources, reportPath);
        FileUtils.cleanDirectory(MiscUtils.getAbsolutePath(TEMP_DIR));

        return reportPath;
    }
}
