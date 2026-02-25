package com.y5neko.ssrtools.models.aiconfig;

/**
 * AI端点配置模型
 * 用于存储OpenAI兼容格式的API端点配置信息
 */
public class AIEndpointConfig {
    // 基本信息
    private String name;              // 配置名称
    private String endpoint;          // API端点URL
    private String apiKey;            // API密钥（明文存储）
    private String model;             // 模型名称
    private String provider;          // 提供商

    // 配置参数
    private boolean isEnabled;        // 是否启用
    private Integer maxTokens;        // 最大token数
    private Double temperature;       // 温度参数
    private Integer connectTimeout;   // 连接超时（秒）
    private Integer readTimeout;      // 读取超时（秒）

    /**
     * 默认构造函数
     * 初始化默认配置值
     */
    public AIEndpointConfig() {
        this.isEnabled = true;
        this.maxTokens = 2000;
        this.temperature = 0.7;
        this.connectTimeout = 30;     // 默认30秒
        this.readTimeout = 60;        // 默认60秒
    }

    /**
     * 构造函数
     * @param name 配置名称
     * @param endpoint API端点URL
     * @param apiKey API密钥
     * @param model 模型名称
     * @param provider 提供商
     */
    public AIEndpointConfig(String name, String endpoint, String apiKey, String model, String provider) {
        this();
        this.name = name;
        this.endpoint = endpoint;
        this.apiKey = apiKey;
        this.model = model;
        this.provider = provider;
    }

    // ================================== getter & setter =============================================

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Integer getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Integer readTimeout) {
        this.readTimeout = readTimeout;
    }

    /**
     * 获取连接超时（毫秒）
     */
    public int getConnectTimeoutMs() {
        return connectTimeout != null ? connectTimeout * 1000 : 30000;
    }

    /**
     * 获取读取超时（毫秒）
     */
    public int getReadTimeoutMs() {
        return readTimeout != null ? readTimeout * 1000 : 60000;
    }
}
