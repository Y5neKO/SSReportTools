package com.y5neko.ssrtools.config;

/**
 * 版权信息
 */
public class CopyRight {
    public static final String VERSION = "1.0.1";

    public static final String COPYRIGHT = "SSReportTools - 安服/渗透测试 报告生成工具\n" +
            "Copyright (c) 2025 Y5neKO\n" +
            "All rights reserved.\n" +
            "本工具仅用于学习和研究，不得用于商业用途。\n" +
            "使用本工具造成的任何损失，均由用户承担。\n" +
            "\n" +
            "版本号: v" + VERSION + "\n" +
            "项目地址: https://github.com/Y5neKO/SSReportTools" + "\n" +
            "================================================";

    public static void showCopyright() {
        System.out.println(COPYRIGHT);
    }
}
