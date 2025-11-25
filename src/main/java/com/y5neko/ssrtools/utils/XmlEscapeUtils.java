package com.y5neko.ssrtools.utils;

public class XmlEscapeUtils {

    /**
     * 转义写入 Word document.xml 的内容，确保不会破坏 XML 结构
     */
    public static String escape(String text) {
        if (text == null) return "";

        StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            switch (c) {
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '&':
                    sb.append("&amp;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&apos;");
                    break;
                default:
                    // 过滤 XML 不允许的控制字符
                    if (c >= 0x00 && c <= 0x1F &&
                            c != '\t' && c != '\n' && c != '\r') {
                        // 跳过
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
