package com.y5neko.ssrtools.config;

/**
 * 全局配置类
 */
public class GlobalConfig {
    public static final String COMPANY_TEMPLATE_DIR = "config/templates";
    public static final String REPORT_TEMPLATE_DIR = "created_templates";
    public static final String VULN_TREE_PATH = "config/vuln_tree.json";
    public static final String VULN_WIKI_FILE_PATH = "config/VulnWiki.yml";
    public static final String DOC_TEMPLATE_PATH = "templates/渗透测试报告模板";
    public static final String TEMP_DIR = "temp";
    public static final String DOC_OUTPUT_DIR = "docs";

    public static final String FIRST_LEVEL_HEADING_TEPMLATE_PATH = "components/first_level_heading.txt";
    public static final String SECOND_LEVEL_HEADING_TEPMLATE_PATH = "components/second_level_heading.txt";
    public static final String THIRD_LEVEL_HEADING_TEPMLATE_PATH = "components/third_level_heading.txt";
    public static final String FOURTH_LEVEL_HEADING_TEPMLATE_PATH = "components/fourth_level_heading.txt";
    public static final String NORMAL_TEXT_TEPMLATE_PATH = "components/normal_text.txt";

    public static final String[] REQUIRED_DIRS = {
            TEMP_DIR,
            DOC_OUTPUT_DIR,
            REPORT_TEMPLATE_DIR
    };
}
