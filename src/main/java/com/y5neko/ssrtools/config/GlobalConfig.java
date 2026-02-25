package com.y5neko.ssrtools.config;

import java.io.File;

/**
 * 全局配置类
 * 统一管理SSReportTools项目的所有路径常量和配置参数
 */
public class GlobalConfig {
    // ==================== 配置文件路径 ====================
    /**
     * 配置文件根目录，存放所有模板、组件和配置文件
     */
    public static final String CONFIG_DIR = "config";

    /**
     * 客户配置模板目录，存放客户信息、项目信息的JSON模板文件
     */
    public static final String COMPANY_TEMPLATE_DIR = CONFIG_DIR + File.separator + "customer-templates";

    /**
     * 漏洞树数据文件，存储已录入的漏洞层级结构数据
     */
    public static final String VULN_TREE_PATH = CONFIG_DIR + File.separator + "vuln_tree.json";

    /**
     * 漏洞知识库文件，基于YAML格式的漏洞详细信息库
     */
    public static final String VULN_WIKI_FILE_PATH = CONFIG_DIR + File.separator + "VulnWiki.yml";

    // ==================== 模板相关路径 ====================
    /**
     * 报告样式模板根目录，存放Word报告的样式模板
     */
    public static final String TEMPLATES_ROOT = CONFIG_DIR + File.separator + "report_templates";

    /**
     * 默认样式模板目录，存放系统内置的Word报告样式模板
     */
    public static final String DEFAULT_TEMPLATE_DIR = TEMPLATES_ROOT + File.separator + "default-styles";

    /**
     * 用户自定义样式模板目录，存放用户创建的Word报告样式模板
     */
    public static final String USER_TEMPLATE_DIR = TEMPLATES_ROOT + File.separator + "user-styles";

    /**
     * 当前使用的文档模板路径，默认指向默认样式模板
     */
    public static final String DOC_TEMPLATE_PATH = DEFAULT_TEMPLATE_DIR; // 保持向后兼容

    // ==================== 标题组件模板路径 ====================
    /**
     * 标题组件模板根目录，存放报告标题的组件模板
     */
    public static final String COMPONENTS_ROOT = CONFIG_DIR + File.separator + "report_components";

    /**
     * 默认标题组件模板目录，存放系统内置的标题组件模板
     */
    public static final String DEFAULT_COMPONENTS_DIR = COMPONENTS_ROOT + File.separator + "default-components";

    /**
     * 用户自定义标题组件模板目录，存放与用户样式模板对应的标题组件
     */
    public static final String USER_COMPONENTS_DIR = COMPONENTS_ROOT + File.separator + "user-components";

    // ==================== AI配置相关路径 ====================
    /**
     * AI配置文件目录，存放AI端点配置JSON文件
     */
    public static final String AI_CONFIG_DIR = CONFIG_DIR + File.separator + "ai-configs";

    /**
     * AI配置文件路径，默认配置文件
     */
    public static final String AI_CONFIG_FILE = AI_CONFIG_DIR + File.separator + "ai_endpoints.json";

    // ==================== 工作空间路径 ====================
    /**
     * 工作空间根目录，存放程序运行时的临时文件和缓存
     */
    public static final String WORKSPACE_DIR = "workspace";

    /**
     * 临时文件目录，存放报告生成过程中的临时文件
     */
    public static final String WORKSPACE_TEMP_DIR = WORKSPACE_DIR + File.separator + "temp";

    /**
     * 缓存目录，存放程序运行时产生的缓存数据
     */
    public static final String WORKSPACE_CACHE_DIR = WORKSPACE_DIR + File.separator + "cache";

    /**
     * 导出目录，存放待导出的文件和数据
     */
    public static final String WORKSPACE_EXPORTS_DIR = WORKSPACE_DIR + File.separator + "exports";

    // ==================== 输出和组件路径 ====================
    /**
     * 文档输出目录，存放最终生成的报告文件（.docx格式）
     */
    public static final String DOC_OUTPUT_DIR = "docs";

    /**
     * 报告组件模板目录（向后兼容，指向默认组件目录）
     */
    public static final String DOC_COMPONENTS_PATH = DEFAULT_COMPONENTS_DIR;

    // ==================== 文档组件模板路径 ====================
    /**
     * 一级标题模板文件，用于生成报告的主要章节标题
     */
    public static final String FIRST_LEVEL_HEADING_TEPMLATE_PATH = DOC_COMPONENTS_PATH + File.separator + "first_level_heading.txt";

    /**
     * 二级标题模板文件，用于生成报告的子章节标题
     */
    public static final String SECOND_LEVEL_HEADING_TEPMLATE_PATH = DOC_COMPONENTS_PATH + File.separator + "second_level_heading.txt";

    /**
     * 三级标题模板文件，用于生成漏洞名称等三级标题
     */
    public static final String THIRD_LEVEL_HEADING_TEPMLATE_PATH = DOC_COMPONENTS_PATH + File.separator + "third_level_heading.txt";

    /**
     * 四级标题模板文件，用于生成漏洞描述、修复建议等四级标题
     */
    public static final String FOURTH_LEVEL_HEADING_TEPMLATE_PATH = DOC_COMPONENTS_PATH + File.separator + "fourth_level_heading.txt";

    /**
     * 正文文本模板文件，用于生成报告的普通文本内容
     */
    public static final String NORMAL_TEXT_TEPMLATE_PATH = DOC_COMPONENTS_PATH + File.separator + "normal_text.txt";

    // ==================== 兼容性路径（保持向后兼容） ====================
    /**
     * @deprecated 使用 {@link #USER_TEMPLATE_DIR} 替代
     * 保留此常量以确保向后兼容性
     */
    @Deprecated
    public static final String REPORT_TEMPLATE_DIR = USER_TEMPLATE_DIR;

    /**
     * @deprecated 使用 {@link #WORKSPACE_CACHE_DIR} 替代
     * 保留此常量以确保向后兼容性
     */
    @Deprecated
    public static final String TEMPLATE_MAKER_CACHE_DIR = WORKSPACE_CACHE_DIR;

    /**
     * @deprecated 使用 {@link #WORKSPACE_TEMP_DIR} 替代
     * 保留此常量以确保向后兼容性
     */
    @Deprecated
    public static final String TEMP_DIR = WORKSPACE_TEMP_DIR;

    // ==================== 必须存在的目录 ====================
    /**
     * 程序运行时必须创建的目录列表
     * 程序启动时会自动检查并创建这些目录，确保正常运行
     */
    public static final String[] REQUIRED_DIRS = {
            CONFIG_DIR,                    // 配置文件根目录
            AI_CONFIG_DIR,                 // AI配置文件目录
            WORKSPACE_DIR,                 // 工作空间根目录
            WORKSPACE_TEMP_DIR,            // 临时文件目录
            WORKSPACE_CACHE_DIR,           // 缓存目录
            WORKSPACE_EXPORTS_DIR,         // 导出目录
            DOC_OUTPUT_DIR,                // 文档输出目录
            DEFAULT_TEMPLATE_DIR,          // 默认样式模板目录
            USER_TEMPLATE_DIR,             // 用户自定义样式目录
            DEFAULT_COMPONENTS_DIR,        // 默认标题组件模板目录
            USER_COMPONENTS_DIR            // 用户自定义标题组件模板目录
    };
}
