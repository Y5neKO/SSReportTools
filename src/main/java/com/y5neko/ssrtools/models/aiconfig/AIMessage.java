package com.y5neko.ssrtools.models.aiconfig;

import java.util.List;
import java.util.Map;

/**
 * AI消息模型
 * 用于构建OpenAI兼容格式的聊天消息
 */
public class AIMessage {
    private String role;       // 角色: system, user, assistant
    private String content;    // 消息内容

    /**
     * 构造函数
     * @param role 消息角色
     * @param content 消息内容
     */
    public AIMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    /**
     * 创建系统消息
     * @param content 消息内容
     * @return 系统消息
     */
    public static AIMessage system(String content) {
        return new AIMessage("system", content);
    }

    /**
     * 创建用户消息
     * @param content 消息内容
     * @return 用户消息
     */
    public static AIMessage user(String content) {
        return new AIMessage("user", content);
    }

    /**
     * 创建助手消息
     * @param content 消息内容
     * @return 助手消息
     */
    public static AIMessage assistant(String content) {
        return new AIMessage("assistant", content);
    }

    // ================================== getter & setter =============================================

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
