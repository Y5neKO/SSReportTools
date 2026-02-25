package com.y5neko.ssrtools.services;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.y5neko.ssrtools.config.GlobalConfig;
import com.y5neko.ssrtools.models.aiconfig.*;
import com.y5neko.ssrtools.utils.LogUtils;
import com.y5neko.ssrtools.utils.MiscUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * AI服务类
 * 提供OpenAI兼容格式的API调用功能
 */
public class AIService {
    private static final int CONNECT_TIMEOUT = 30000;  // 连接超时30秒
    private static final int READ_TIMEOUT = 60000;     // 读取超时60秒

    // 测试连接专用超时（更短，快速验证）
    private static final int TEST_CONNECT_TIMEOUT = 5000;  // 测试连接超时5秒
    private static final int TEST_READ_TIMEOUT = 10000;     // 测试读取超时10秒

    /**
     * AI服务异常
     */
    public static class AIServiceException extends Exception {
        public AIServiceException(String message) {
            super(message);
        }

        public AIServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // ==================== 配置管理 ====================

    /**
     * 加载AI端点配置列表
     * @return AI端点配置列表
     * @throws AIServiceException 加载失败时抛出
     */
    public static List<AIEndpointConfig> loadEndpointConfigs() throws AIServiceException {
        String configPath = MiscUtils.getAbsolutePath(GlobalConfig.AI_CONFIG_FILE);
        File file = new File(configPath);

        if (!file.exists()) {
            LogUtils.info(AIService.class, "AI配置文件不存在，返回空列表: " + configPath);
            return new ArrayList<>();
        }

        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            JSONObject jsonObject = JSON.parseObject(content);
            List<AIEndpointConfig> endpoints = jsonObject.getList("endpoints", AIEndpointConfig.class);

            if (endpoints != null) {
                LogUtils.info(AIService.class, "加载AI配置成功，共 " + endpoints.size() + " 个配置");
                return endpoints;
            }
            return new ArrayList<>();
        } catch (IOException e) {
            LogUtils.error(AIService.class, "加载AI配置失败", e);
            throw new AIServiceException("加载AI配置失败: " + e.getMessage(), e);
        }
    }

    /**
     * 保存AI端点配置列表
     * @param configs AI端点配置列表
     * @throws AIServiceException 保存失败时抛出
     */
    public static void saveEndpointConfigs(List<AIEndpointConfig> configs) throws AIServiceException {
        String configPath = MiscUtils.getAbsolutePath(GlobalConfig.AI_CONFIG_FILE);
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("endpoints", configs);

            String jsonStr = JSON.toJSONString(jsonObject, com.alibaba.fastjson2.JSONWriter.Feature.PrettyFormat);

            // 确保目录存在
            File file = new File(configPath);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            Files.write(file.toPath(), jsonStr.getBytes());
            LogUtils.info(AIService.class, "保存AI配置成功");
        } catch (IOException e) {
            LogUtils.error(AIService.class, "保存AI配置失败", e);
            throw new AIServiceException("保存AI配置失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取当前启用的AI端点配置
     * @return 启用的配置，如果没有启用的配置则返回null
     * @throws AIServiceException 加载失败时抛出
     */
    public static AIEndpointConfig getEnabledEndpointConfig() throws AIServiceException {
        List<AIEndpointConfig> configs = loadEndpointConfigs();
        for (AIEndpointConfig config : configs) {
            if (config.isEnabled()) {
                return config;
            }
        }
        return null;
    }

    // ==================== API调用 ====================

    /**
     * 发送聊天请求到AI服务
     * @param request 聊天请求
     * @param config AI端点配置
     * @return 聊天响应
     * @throws AIServiceException 请求失败时抛出
     */
    public static AIChatResponse chat(AIChatRequest request, AIEndpointConfig config) throws AIServiceException {
        return chat(request, config, CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    /**
     * 发送聊天请求到AI服务（支持自定义超时）
     * @param request 聊天请求
     * @param config AI端点配置
     * @param connectTimeout 连接超时（毫秒）
     * @param readTimeout 读取超时（毫秒）
     * @return 聊天响应
     * @throws AIServiceException 请求失败时抛出
     */
    public static AIChatResponse chat(AIChatRequest request, AIEndpointConfig config,
                                      int connectTimeout, int readTimeout) throws AIServiceException {
        if (config == null) {
            throw new AIServiceException("AI端点配置不能为空");
        }

        String endpoint = config.getEndpoint();
        String apiKey = config.getApiKey();

        if (endpoint == null || endpoint.isEmpty()) {
            throw new AIServiceException("API端点地址不能为空");
        }

        if (apiKey == null || apiKey.isEmpty()) {
            throw new AIServiceException("API密钥不能为空");
        }

        try {
            // 构建请求体
            String requestBody = buildRequestBody(request, config);

            // 发送HTTP请求（使用自定义超时）
            String responseBody = sendHttpRequest(endpoint, apiKey, requestBody, connectTimeout, readTimeout);

            // 解析响应
            return parseResponse(responseBody);
        } catch (IOException e) {
            LogUtils.error(AIService.class, "AI API调用失败", e);
            throw new AIServiceException("AI API调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用当前启用的配置发送聊天请求
     * @param request 聊天请求
     * @return 聊天响应
     * @throws AIServiceException 请求失败时抛出
     */
    public static AIChatResponse chat(AIChatRequest request) throws AIServiceException {
        AIEndpointConfig config = getEnabledEndpointConfig();
        if (config == null) {
            throw new AIServiceException("没有启用的AI配置，请先在AI配置管理中启用一个配置");
        }
        return chat(request, config);
    }

    /**
     * 发送简单的文本消息
     * @param message 消息内容
     * @return AI回复内容
     * @throws AIServiceException 请求失败时抛出
     */
    public static String chat(String message) throws AIServiceException {
        List<AIMessage> messages = new ArrayList<>();
        messages.add(AIMessage.user(message));
        return chat(messages);
    }

    /**
     * 发送消息列表
     * @param messages 消息列表
     * @return AI回复内容
     * @throws AIServiceException 请求失败时抛出
     */
    public static String chat(List<AIMessage> messages) throws AIServiceException {
        AIEndpointConfig config = getEnabledEndpointConfig();
        if (config == null) {
            throw new AIServiceException("没有启用的AI配置，请先在AI配置管理中启用一个配置");
        }

        AIChatRequest request = new AIChatRequest(config.getModel(), messages);
        request.setTemperature(config.getTemperature());
        request.setMaxTokens(config.getMaxTokens());

        AIChatResponse response = chat(request, config);

        if (response.hasError()) {
            throw new AIServiceException("AI服务返回错误: " + response.getError().getMessage());
        }

        String content = response.getContent();
        if (content == null) {
            throw new AIServiceException("AI服务返回空响应");
        }

        return content;
    }

    /**
     * 测试AI端点连接
     * @param config AI端点配置
     * @return 测试结果消息
     */
    public static String testConnection(AIEndpointConfig config) {
        try {
            // 构建简单的测试请求
            List<AIMessage> messages = new ArrayList<>();
            messages.add(AIMessage.user("Hi"));  // 最简化的测试消息

            AIChatRequest request = new AIChatRequest(config.getModel(), messages);
            request.setTemperature(0.1);  // 使用低温度值，加快响应速度
            request.setMaxTokens(10);     // 最小token数限制

            // 使用短超时进行测试连接
            AIChatResponse response = chat(request, config, TEST_CONNECT_TIMEOUT, TEST_READ_TIMEOUT);

            if (response.hasError()) {
                return "连接失败: " + response.getError().getMessage();
            }

            return "连接成功! " +
                   (response.getUsage() != null ?
                           "(Token: " + response.getUsage().getTotalTokens() + ")" : "");
        } catch (AIServiceException e) {
            // 处理超时等异常
            String msg = e.getMessage();
            if (msg != null && (msg.contains("timeout") || msg.contains("timed out") || msg.contains("超时"))) {
                return "连接超时: 请检查网络或端点地址是否正确";
            }
            return "连接失败: " + e.getMessage();
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 构建请求体
     */
    private static String buildRequestBody(AIChatRequest request, AIEndpointConfig config) {
        // 使用配置中的参数覆盖请求中的参数
        AIChatRequest mergedRequest = new AIChatRequest();
        mergedRequest.setModel(config.getModel() != null ? config.getModel() : request.getModel());
        mergedRequest.setMessages(request.getMessages());
        mergedRequest.setTemperature(config.getTemperature() != null ? config.getTemperature() : request.getTemperature());
        mergedRequest.setMaxTokens(config.getMaxTokens() != null ? config.getMaxTokens() : request.getMaxTokens());

        return JSON.toJSONString(mergedRequest);
    }

    /**
     * 发送HTTP请求
     */
    private static String sendHttpRequest(String endpoint, String apiKey, String requestBody) throws IOException {
        return sendHttpRequest(endpoint, apiKey, requestBody, CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    /**
     * 发送HTTP请求（支持自定义超时）
     */
    private static String sendHttpRequest(String endpoint, String apiKey, String requestBody,
                                          int connectTimeout, int readTimeout) throws IOException {
        URL url = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            // 设置请求方法和头
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(connectTimeout);
            connection.setReadTimeout(readTimeout);
            connection.setDoOutput(true);

            // 发送请求体
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // 读取响应
            int responseCode = connection.getResponseCode();
            BufferedReader br;

            if (responseCode >= 200 && responseCode < 300) {
                br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
            } else {
                br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8"));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line.trim());
            }
            br.close();

            if (responseCode >= 200 && responseCode < 300) {
                return response.toString();
            } else {
                throw new IOException("HTTP请求失败，状态码: " + responseCode + ", 响应: " + response.toString());
            }
        } finally {
            connection.disconnect();
        }
    }

    /**
     * 解析响应
     */
    private static AIChatResponse parseResponse(String responseBody) {
        return JSON.parseObject(responseBody, AIChatResponse.class);
    }
}
