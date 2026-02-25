package com.y5neko.ssrtools.models.aiconfig;

import java.util.List;

/**
 * AI聊天请求模型
 * 用于构建OpenAI兼容格式的聊天请求
 */
public class AIChatRequest {
    private String model;                   // 模型名称
    private List<AIMessage> messages;       // 消息列表
    private Double temperature;             // 温度参数 (0-2)
    private Integer maxTokens;              // 最大生成token数
    private Double topP;                    // top-p采样参数
    private Double frequencyPenalty;        // 频率惩罚
    private Double presencePenalty;         // 存在惩罚
    private List<String> stop;              // 停止序列

    /**
     * 默认构造函数
     */
    public AIChatRequest() {
        this.temperature = 0.7;
        this.maxTokens = 2000;
        this.topP = 1.0;
        this.frequencyPenalty = 0.0;
        this.presencePenalty = 0.0;
    }

    /**
     * 构造函数
     * @param model 模型名称
     * @param messages 消息列表
     */
    public AIChatRequest(String model, List<AIMessage> messages) {
        this();
        this.model = model;
        this.messages = messages;
    }

    /**
     * 构建器模式
     */
    public static class Builder {
        private AIChatRequest request;

        public Builder() {
            this.request = new AIChatRequest();
        }

        public Builder model(String model) {
            request.setModel(model);
            return this;
        }

        public Builder messages(List<AIMessage> messages) {
            request.setMessages(messages);
            return this;
        }

        public Builder temperature(Double temperature) {
            request.setTemperature(temperature);
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            request.setMaxTokens(maxTokens);
            return this;
        }

        public Builder topP(Double topP) {
            request.setTopP(topP);
            return this;
        }

        public AIChatRequest build() {
            return request;
        }
    }

    /**
     * 获取构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    // ================================== getter & setter =============================================

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<AIMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<AIMessage> messages) {
        this.messages = messages;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Double getTopP() {
        return topP;
    }

    public void setTopP(Double topP) {
        this.topP = topP;
    }

    public Double getFrequencyPenalty() {
        return frequencyPenalty;
    }

    public void setFrequencyPenalty(Double frequencyPenalty) {
        this.frequencyPenalty = frequencyPenalty;
    }

    public Double getPresencePenalty() {
        return presencePenalty;
    }

    public void setPresencePenalty(Double presencePenalty) {
        this.presencePenalty = presencePenalty;
    }

    public List<String> getStop() {
        return stop;
    }

    public void setStop(List<String> stop) {
        this.stop = stop;
    }
}
