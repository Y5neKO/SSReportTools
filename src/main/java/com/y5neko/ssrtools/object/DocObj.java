package com.y5neko.ssrtools.object;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 文档对象
 */
public class DocObj {
    private String customerName = "XX客户";
    private String isFirsrTest = "初测";
    private String signatureName = "XX安全公司";
    private String reportTime = "2025年8月7日";
    private String reporter = "Y6n4K1";
    private String testTime = "2025年8月6日";
    private String testerName = "Y5neKO";
    private String pmName = "PM";
    private String templateName = "默认模板";

    private int vulAllCount = 9;
    private int vulHighCount = 4;
    private int vulMediumCount = 3;
    private int vulLowCount = 2;

    private int reportYear;
    private int reportMonth;
    private int reportDay;

    public DocObj() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年M月d日");
        LocalDate date = LocalDate.parse(reportTime, formatter);

        reportYear = date.getYear();
        reportMonth = date.getMonthValue();
        reportDay = date.getDayOfMonth();
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getIsFirsrTest() {
        return isFirsrTest;
    }

    public void setIsFirsrTest(String isFirsrTest) {
        this.isFirsrTest = isFirsrTest;
    }

    public String getSignatureName() {
        return signatureName;
    }

    public void setSignatureName(String signatureName) {
        this.signatureName = signatureName;
    }

    public String getReportTime() {
        return reportTime;
    }

    public void setReportTime(String reportTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年M月d日");
        LocalDate date = LocalDate.parse(reportTime, formatter);

        reportYear = date.getYear();
        reportMonth = date.getMonthValue();
        reportDay = date.getDayOfMonth();

        this.reportTime = reportTime;
    }

    public String getReporter() {
        return reporter;
    }

    public void setReporter(String reporter) {
        this.reporter = reporter;
    }

    public String getTestTime() {
        return testTime;
    }

    public void setTestTime(String testTime) {
        this.testTime = testTime;
    }

    public int getVulAllCount() {
        return vulAllCount;
    }

    public void setVulAllCount(int vulAllCount) {
        this.vulAllCount = vulAllCount;
    }

    public int getVulHighCount() {
        return vulHighCount;
    }

    public void setVulHighCount(int vulHighCount) {
        this.vulHighCount = vulHighCount;
    }

    public int getVulMediumCount() {
        return vulMediumCount;
    }

    public void setVulMediumCount(int vulMediumCount) {
        this.vulMediumCount = vulMediumCount;
    }

    public int getVulLowCount() {
        return vulLowCount;
    }

    public void setVulLowCount(int vulLowCount) {
        this.vulLowCount = vulLowCount;
    }

    public int getReportYear() {
        return reportYear;
    }

    public int getReportMonth() {
        return reportMonth;
    }

    public int getReportDay() {
        return reportDay;
    }

    public String getTesterName() {
        return testerName;
    }

    public void setTesterName(String testerName) {
        this.testerName = testerName;
    }

    public String getPmName() {
        return pmName;
    }

    public void setPmName(String pmName) {
        this.pmName = pmName;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }
}
