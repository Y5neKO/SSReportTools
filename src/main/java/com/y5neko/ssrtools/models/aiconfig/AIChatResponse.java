package com.y5neko.ssrtools.models.aiconfig;

import java.util.List;

/**
 * AI聊天响应模型
 * 用于解析OpenAI兼容格式的聊天响应
 */
public class AIChatResponse {
    private String id;                      // 响应ID
    private String object;                  // 对象类型
    private Long created;                   // 创建时间戳
    private String model;                   // 使用的模型
    private List<Choice> choices;           // 候选回复列表
    private Usage usage;                    // token使用情况
    private Error error;                    // 错误信息

    /**
     * 候选回复
     */
    public static class Choice {
        private Integer index;
        private AIMessage message;
        private String finishReason;        // 结束原因: stop, length, content_filter

        public Integer getIndex() {
            return index;
        }

        public void setIndex(Integer index) {
            this.index = index;
        }

        public AIMessage getMessage() {
            return message;
        }

        public void setMessage(AIMessage message) {
            this.message = message;
        }

        public String getFinishReason() {
            return finishReason;
        }

        public void setFinishReason(String finishReason) {
            this.finishReason = finishReason;
        }
    }

    /**
     * Token使用情况
     */
    public static class Usage {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;

        public Integer getPromptTokens() {
            return promptTokens;
        }

        public void setPromptTokens(Integer promptTokens) {
            this.promptTokens = promptTokens;
        }

        public Integer getCompletionTokens() {
            return completionTokens;
        }

        public void setCompletionTokens(Integer completionTokens) {
            this.completionTokens = completionTokens;
        }

        public Integer getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(Integer totalTokens) {
            this.totalTokens = totalTokens;
        }
    }

    /**
     * 错误信息
     */
    public static class Error {
        private String code;
        private String message;
        private String type;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    // ================================== getter & setter =============================================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Choice> getChoices() {
        return choices;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }

    public Usage getUsage() {
        return usage;
    }

    public void setUsage(Usage usage) {
        this.usage = usage;
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }

    /**
     * 获取第一个候选回复的内容
     * @return 回复内容，如果没有则返回null
     */
    public String getContent() {
        if (choices != null && !choices.isEmpty()) {
            Choice choice = choices.get(0);
            if (choice.getMessage() != null) {
                return choice.getMessage().getContent();
            }
        }
        return null;
    }

    /**
     * 检查响应是否包含错误
     * @return 如果包含错误返回true
     */
    public boolean hasError() {
        return error != null;
    }
}
