package com.y5neko.ssrtools.ui;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.y5neko.ssrtools.models.aiconfig.AIEndpointConfig;
import com.y5neko.ssrtools.models.aiconfig.AIMessage;
import com.y5neko.ssrtools.services.AIService;
import com.y5neko.ssrtools.utils.LogUtils;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * AI辅助生成漏洞信息窗口
 * 允许用户输入漏洞名称和补充说明，通过AI自动生成漏洞的危害、等级、描述和修复建议
 */
public class VulnAIGeneratorWindow {

    // ==================== AI Prompt 常量 ====================

    /**
     * 系统提示词 - 定义AI的角色和输出格式
     */
    private static final String SYSTEM_PROMPT = "你是一位专业的网络安全专家，专注于渗透测试和漏洞分析。你的任务是根据用户提供的漏洞名称和补充说明，生成专业、准确的漏洞信息。\n\n" +
            "## 输出格式要求\n\n" +
            "**必须** 严格按照以下JSON格式返回结果，**不要包含任何其他内容**（不要包含```json等markdown标记）：\n" +
            "{\n" +
            "  \"harm\": \"漏洞危害描述\",\n" +
            "  \"riskLevel\": \"风险等级\",\n" +
            "  \"description\": \"漏洞详细描述\",\n" +
            "  \"suggestion\": \"修复建议\"\n" +
            "}\n\n" +
            "## 字段说明\n\n" +
            "- **harm**: 详细说明该漏洞可能造成的安全影响，包括数据泄露、系统被控、权限提升等具体后果\n" +
            "- **riskLevel**: 风险等级，**仅限以下三个值之一**: \"高危\"、\"中危\"、\"低危\"\n" +
            "- **description**: 漏洞详细描述，包含漏洞原理、触发条件、影响范围、攻击向量等技术细节\n" +
            "- **suggestion**: 修复建议，**必须使用以下编号列表格式，每条建议单独一行**:\n" +
            "  1、第一条建议；\n" +
            "  2、第二条建议；\n" +
            "  3、第三条建议。\n\n" +
            "## 重要约束\n\n" +
            "1. 所有字段都必须填写，不能为空\n" +
            "2. 仅返回纯JSON字符串，**不要包含markdown代码块标记(```json等)**\n" +
            "3. 内容应当专业、详细，适合用于正式的安全测试报告";

    // ==================== UI组件 ====================

    private Stage stage;
    private VulnAIResultCallback callback;

    // 输入控件
    private TextField vulnNameField;
    private TextArea additionalInfoArea;

    // 结果展示控件（可编辑）
    private TextArea harmArea;
    private ComboBox<String> riskLevelBox;
    private TextArea descArea;
    private TextArea suggestionArea;

    // 按钮
    private Button generateBtn;
    private Button applyBtn;
    private Button closeBtn;

    // 加载状态标签
    private Label statusLabel;

    // ==================== 样式常量 ====================

    private static final String INPUT_STYLE = "-fx-font-size: 12px; -fx-padding: 5px 8px; -fx-border-radius: 4px; " +
            "-fx-border-color: #dfe6e9; -fx-border-width: 1px; -fx-background-radius: 4px; " +
            "-fx-focus-color: transparent; -fx-faint-focus-color: transparent;";

    private static final String LABEL_STYLE = "-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #636e72;";

    private static final String AI_BTN_STYLE = "-fx-background-color: linear-gradient(to right, #667eea, #764ba2); " +
            "-fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 5px; -fx-padding: 6px 14px; " +
            "-fx-font-size: 12px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; " +
            "-fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(102, 126, 234, 0.3), 4, 0, 0, 1);";

    private static final String AI_BTN_HOVER_STYLE = "-fx-background-color: linear-gradient(to right, #5a6fd6, #6a4192); " +
            "-fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 5px; -fx-padding: 6px 14px; " +
            "-fx-font-size: 12px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; " +
            "-fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(102, 126, 234, 0.5), 6, 0, 0, 2);";

    private static final String AI_BTN_DISABLED_STYLE = "-fx-background-color: linear-gradient(to right, #a0a0a0, #909090); " +
            "-fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 5px; -fx-padding: 6px 14px; " +
            "-fx-font-size: 12px; -fx-cursor: wait; -fx-border-width: 1px; -fx-border-color: transparent; " +
            "-fx-background-insets: 0;";

    private static final String SUCCESS_BTN_STYLE = "-fx-background-color: #26de81; -fx-text-fill: white; " +
            "-fx-font-weight: 600; -fx-border-radius: 5px; -fx-padding: 5px 12px; -fx-font-size: 11px; " +
            "-fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; " +
            "-fx-effect: dropshadow(gaussian, rgba(38, 222, 129, 0.2), 3, 0, 0, 1);";

    private static final String SUCCESS_BTN_HOVER_STYLE = "-fx-background-color: #1eb980; -fx-text-fill: white; " +
            "-fx-font-weight: 600; -fx-border-radius: 5px; -fx-padding: 5px 12px; -fx-font-size: 11px; " +
            "-fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; " +
            "-fx-effect: dropshadow(gaussian, rgba(38, 222, 129, 0.4), 5, 0, 0, 2);";

    private static final String SECONDARY_BTN_STYLE = "-fx-background-color: #b2bec3; -fx-text-fill: white; " +
            "-fx-font-weight: 600; -fx-border-radius: 5px; -fx-padding: 5px 12px; -fx-font-size: 11px; " +
            "-fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; " +
            "-fx-effect: dropshadow(gaussian, rgba(178, 190, 195, 0.2), 3, 0, 0, 1);";

    private static final String SECONDARY_BTN_HOVER_STYLE = "-fx-background-color: #95a5a6; -fx-text-fill: white; " +
            "-fx-font-weight: 600; -fx-border-radius: 5px; -fx-padding: 5px 12px; -fx-font-size: 11px; " +
            "-fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; " +
            "-fx-effect: dropshadow(gaussian, rgba(178, 190, 195, 0.4), 5, 0, 0, 2);";

    // ==================== 构造函数 ====================

    /**
     * 构造函数
     * @param initialVulnName 初始漏洞名称（可从父窗口传入）
     * @param callback 结果回调接口
     */
    public VulnAIGeneratorWindow(String initialVulnName, VulnAIResultCallback callback) {
        this.callback = callback;
        initControls();
        createStage();

        // 设置初始漏洞名称
        if (initialVulnName != null && !initialVulnName.isEmpty()) {
            vulnNameField.setText(initialVulnName);
        }
    }

    // ==================== 初始化方法 ====================

    /**
     * 初始化控件
     */
    private void initControls() {
        // 输入控件
        vulnNameField = new TextField();
        vulnNameField.setPromptText("请输入漏洞名称，如：SQL注入漏洞");
        vulnNameField.setStyle(INPUT_STYLE);
        vulnNameField.setPrefWidth(400);

        additionalInfoArea = new TextArea();
        additionalInfoArea.setPromptText("可选：补充说明...");
        additionalInfoArea.setPrefRowCount(2);
        additionalInfoArea.setWrapText(true);
        additionalInfoArea.setStyle(INPUT_STYLE);

        // 结果展示控件（可编辑）
        harmArea = new TextArea();
        harmArea.setPromptText("漏洞危害...");
        harmArea.setPrefRowCount(2);
        harmArea.setWrapText(true);
        harmArea.setStyle(INPUT_STYLE);
        harmArea.setDisable(true);

        riskLevelBox = new ComboBox<>();
        riskLevelBox.getItems().addAll("高危", "中危", "低危");
        riskLevelBox.setPromptText("风险等级");
        riskLevelBox.setStyle("-fx-font-size: 11px; -fx-padding: 4px 8px; -fx-border-radius: 4px; " +
                "-fx-border-color: #dfe6e9; -fx-border-width: 1px; -fx-background-radius: 4px; " +
                "-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        riskLevelBox.setPrefWidth(120);
        riskLevelBox.setDisable(true);

        descArea = new TextArea();
        descArea.setPromptText("漏洞描述...");
        descArea.setPrefRowCount(2);
        descArea.setWrapText(true);
        descArea.setStyle(INPUT_STYLE);
        descArea.setDisable(true);

        suggestionArea = new TextArea();
        suggestionArea.setPromptText("修复建议...");
        suggestionArea.setPrefRowCount(2);
        suggestionArea.setWrapText(true);
        suggestionArea.setStyle(INPUT_STYLE);
        suggestionArea.setDisable(true);

        // 状态标签
        statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #636e72;");
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        // 按钮
        generateBtn = new Button("AI生成漏洞信息");
        generateBtn.setStyle(AI_BTN_STYLE);
        generateBtn.setOnMouseEntered(e -> {
            if (!generateBtn.isDisabled()) {
                generateBtn.setStyle(AI_BTN_HOVER_STYLE);
            }
        });
        generateBtn.setOnMouseExited(e -> {
            if (!generateBtn.isDisabled()) {
                generateBtn.setStyle(AI_BTN_STYLE);
            }
        });

        applyBtn = new Button("应用到编辑器");
        applyBtn.setStyle(SUCCESS_BTN_STYLE);
        applyBtn.setDisable(true);
        applyBtn.setOnMouseEntered(e -> {
            if (!applyBtn.isDisabled()) {
                applyBtn.setStyle(SUCCESS_BTN_HOVER_STYLE);
            }
        });
        applyBtn.setOnMouseExited(e -> {
            if (!applyBtn.isDisabled()) {
                applyBtn.setStyle(SUCCESS_BTN_STYLE);
            }
        });

        closeBtn = new Button("关闭");
        closeBtn.setStyle(SECONDARY_BTN_STYLE);
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(SECONDARY_BTN_HOVER_STYLE));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(SECONDARY_BTN_STYLE));
    }

    /**
     * 创建窗口
     */
    private void createStage() {
        stage = new Stage();
        stage.setTitle("AI辅助生成漏洞信息");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #f5f7fa;");

        VBox mainContainer = new VBox(6);
        mainContainer.setPadding(new Insets(8));
        mainContainer.setStyle("-fx-background-color: white; -fx-border-color: #dfe6e9; " +
                "-fx-border-radius: 8px; -fx-background-radius: 8px;");

        // 标题
        Label titleLabel = new Label("AI辅助生成漏洞信息");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #2d3436;");

        // 输入区域
        VBox inputSection = createInputSection();

        // 分隔线
        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: #dfe6e9;");

        // 结果区域
        VBox resultSection = createResultSection();

        // 按钮区域
        HBox buttonBox = createButtonBox();

        mainContainer.getChildren().addAll(
                titleLabel,
                inputSection,
                separator,
                resultSection,
                buttonBox
        );

        root.setCenter(mainContainer);

        // 绑定事件
        bindEvents();

        Scene scene = new Scene(root, 480, 600);
        stage.setMinWidth(450);
        stage.setMinHeight(550);
        stage.setScene(scene);
    }

    /**
     * 创建输入区域
     */
    private VBox createInputSection() {
        VBox section = new VBox(8);

        // 漏洞名称
        Label vulnNameLabel = new Label("漏洞名称 *");
        vulnNameLabel.setStyle(LABEL_STYLE);

        // 补充说明
        Label additionalLabel = new Label("补充说明 (可选)");
        additionalLabel.setStyle(LABEL_STYLE);

        // 生成按钮容器
        HBox generateBox = new HBox(10);
        generateBox.setAlignment(Pos.CENTER);
        generateBox.getChildren().addAll(generateBtn, statusLabel);
        generateBox.setPadding(new Insets(5, 0, 5, 0));

        section.getChildren().addAll(
                vulnNameLabel, vulnNameField,
                additionalLabel, additionalInfoArea,
                generateBox
        );

        return section;
    }

    /**
     * 创建结果区域
     */
    private VBox createResultSection() {
        VBox section = new VBox(8);

        // 结果标题
        Label resultTitle = new Label("生成结果");
        resultTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2d3436;");

        // 漏洞危害
        Label harmLabel = new Label("漏洞危害");
        harmLabel.setStyle(LABEL_STYLE);

        // 风险等级
        HBox riskBox = new HBox(10);
        riskBox.setAlignment(Pos.CENTER_LEFT);
        Label riskLabel = new Label("风险等级");
        riskLabel.setStyle(LABEL_STYLE);
        riskBox.getChildren().addAll(riskLabel, riskLevelBox);

        // 漏洞描述
        Label descLabel = new Label("漏洞描述");
        descLabel.setStyle(LABEL_STYLE);

        // 修复建议
        Label suggestionLabel = new Label("修复建议");
        suggestionLabel.setStyle(LABEL_STYLE);

        section.getChildren().addAll(
                resultTitle,
                harmLabel, harmArea,
                riskBox,
                descLabel, descArea,
                suggestionLabel, suggestionArea
        );

        // 设置文本区域扩展
        VBox.setVgrow(harmArea, Priority.NEVER);
        VBox.setVgrow(descArea, Priority.ALWAYS);
        VBox.setVgrow(suggestionArea, Priority.ALWAYS);

        return section;
    }

    /**
     * 创建按钮区域
     */
    private HBox createButtonBox() {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(8, 0, 0, 0));
        box.getChildren().addAll(applyBtn, closeBtn);
        return box;
    }

    /**
     * 绑定事件
     */
    private void bindEvents() {
        // 生成按钮事件
        generateBtn.setOnAction(e -> onGenerate());

        // 应用按钮事件
        applyBtn.setOnAction(e -> onApply());

        // 关闭按钮事件
        closeBtn.setOnAction(e -> stage.close());
    }

    // ==================== 业务逻辑方法 ====================

    /**
     * 生成按钮点击事件
     */
    private void onGenerate() {
        String vulnName = vulnNameField.getText().trim();

        // 验证漏洞名称
        if (vulnName.isEmpty()) {
            showErrorAlert("请输入漏洞名称");
            vulnNameField.requestFocus();
            return;
        }

        // 检查AI配置
        try {
            AIEndpointConfig config = AIService.getEnabledEndpointConfig();
            if (config == null) {
                showErrorAlert("没有启用的AI配置，请先在AI配置管理中启用一个配置");
                return;
            }
        } catch (AIService.AIServiceException e) {
            showErrorAlert("加载AI配置失败: " + e.getMessage());
            LogUtils.error(VulnAIGeneratorWindow.class, "加载AI配置失败", e);
            return;
        }

        // 更新UI状态为加载中
        setLoadingState(true);

        // 获取补充说明
        String additionalInfo = additionalInfoArea.getText().trim();

        // 在后台线程执行AI调用
        new Thread(() -> {
            try {
                // 构建用户提示词
                String userPrompt = buildUserPrompt(vulnName, additionalInfo);

                // 构建消息列表
                List<AIMessage> messages = new ArrayList<>();
                messages.add(AIMessage.system(SYSTEM_PROMPT));
                messages.add(AIMessage.user(userPrompt));

                // 调用AI服务
                String response = AIService.chat(messages);

                // 解析响应
                VulnAIResult result = parseAIResponse(response);

                // 在UI线程更新界面
                Platform.runLater(() -> {
                    updateResultArea(result);
                    setLoadingState(false);
                    enableResultEditing(true);
                    showInfoAlert("AI生成成功！您可以编辑内容后应用到编辑器。");
                });

                LogUtils.info(VulnAIGeneratorWindow.class, "AI生成漏洞信息成功: " + vulnName);

            } catch (AIService.AIServiceException e) {
                Platform.runLater(() -> {
                    setLoadingState(false);
                    showErrorAlert("AI生成失败: " + e.getMessage());
                });
                LogUtils.error(VulnAIGeneratorWindow.class, "AI生成失败", e);
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setLoadingState(false);
                    showErrorAlert("发生未知错误: " + e.getMessage());
                });
                LogUtils.error(VulnAIGeneratorWindow.class, "AI生成发生未知错误", e);
            }
        }).start();
    }

    /**
     * 构建用户提示词
     * @param vulnName 漏洞名称
     * @param additionalInfo 补充说明
     * @return 用户提示词
     */
    private String buildUserPrompt(String vulnName, String additionalInfo) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请为以下漏洞生成详细信息：\n\n");
        prompt.append("漏洞名称：").append(vulnName).append("\n");

        if (additionalInfo != null && !additionalInfo.isEmpty()) {
            prompt.append("\n补充说明：\n").append(additionalInfo).append("\n");
        }

        prompt.append("\n---\n");
        prompt.append("请直接返回JSON格式的漏洞信息，不要包含任何解释文本或markdown标记。\n");
        prompt.append("suggestion字段必须使用编号列表格式，每条建议换行:\n1、xxx；\n2、xxx；\n3、xxx。");
        return prompt.toString();
    }

    /**
     * 解析AI响应
     * @param response AI响应字符串
     * @return 解析后的漏洞信息
     */
    private VulnAIResult parseAIResponse(String response) {
        // 提取JSON部分（处理可能的markdown代码块）
        String json = response;

        if (response.contains("```json")) {
            int start = response.indexOf("```json") + 7;
            int end = response.lastIndexOf("```");
            if (end > start) {
                json = response.substring(start, end).trim();
            }
        } else if (response.contains("```")) {
            int start = response.indexOf("```") + 3;
            int end = response.lastIndexOf("```");
            if (end > start) {
                json = response.substring(start, end).trim();
            }
        }

        // 尝试找到JSON对象的起始和结束位置
        int jsonStart = json.indexOf("{");
        int jsonEnd = json.lastIndexOf("}");
        if (jsonStart != -1 && jsonEnd > jsonStart) {
            json = json.substring(jsonStart, jsonEnd + 1);
        }

        // 解析JSON
        JSONObject jsonObject = JSON.parseObject(json);

        VulnAIResult result = new VulnAIResult();
        result.setHarm(jsonObject.getString("harm"));
        result.setRiskLevel(jsonObject.getString("riskLevel"));
        result.setDescription(jsonObject.getString("description"));
        result.setSuggestion(jsonObject.getString("suggestion"));

        return result;
    }

    /**
     * 更新结果显示区域
     * @param result AI生成的结果
     */
    private void updateResultArea(VulnAIResult result) {
        harmArea.setText(result.getHarm() != null ? result.getHarm() : "");
        descArea.setText(result.getDescription() != null ? result.getDescription() : "");
        suggestionArea.setText(result.getSuggestion() != null ? result.getSuggestion() : "");

        // 设置风险等级
        if (result.getRiskLevel() != null) {
            String riskLevel = result.getRiskLevel().trim();
            if (riskLevelBox.getItems().contains(riskLevel)) {
                riskLevelBox.setValue(riskLevel);
            } else {
                // 如果AI返回的等级不在预设列表中，尝试匹配
                if (riskLevel.contains("高") || riskLevel.toLowerCase().contains("high")) {
                    riskLevelBox.setValue("高危");
                } else if (riskLevel.contains("低") || riskLevel.toLowerCase().contains("low")) {
                    riskLevelBox.setValue("低危");
                } else {
                    riskLevelBox.setValue("中危");
                }
            }
        }

        // 启用应用按钮
        applyBtn.setDisable(false);
    }

    /**
     * 设置加载状态
     * @param loading 是否加载中
     */
    private void setLoadingState(boolean loading) {
        if (loading) {
            generateBtn.setText("生成中...");
            generateBtn.setDisable(true);
            generateBtn.setStyle(AI_BTN_DISABLED_STYLE);
            statusLabel.setText("正在调用AI服务，请稍候...");
            statusLabel.setVisible(true);
            statusLabel.setManaged(true);
        } else {
            generateBtn.setText("AI生成漏洞信息");
            generateBtn.setDisable(false);
            generateBtn.setStyle(AI_BTN_STYLE);
            statusLabel.setVisible(false);
            statusLabel.setManaged(false);
        }
    }

    /**
     * 启用结果区域编辑
     * @param editable 是否可编辑
     */
    private void enableResultEditing(boolean editable) {
        harmArea.setDisable(!editable);
        riskLevelBox.setDisable(!editable);
        descArea.setDisable(!editable);
        suggestionArea.setDisable(!editable);
    }

    /**
     * 应用按钮点击事件
     */
    private void onApply() {
        // 获取编辑后的内容
        String vulnName = vulnNameField.getText().trim();
        String harm = harmArea.getText();
        String riskLevel = riskLevelBox.getValue();
        String description = descArea.getText();
        String suggestion = suggestionArea.getText();

        // 验证必填字段
        if (harm.trim().isEmpty() && description.trim().isEmpty() &&
            suggestion.trim().isEmpty() && riskLevel == null) {
            showErrorAlert("没有可应用的内容，请先生成漏洞信息");
            return;
        }

        // 回调到父窗口
        if (callback != null) {
            callback.onApplyResult(
                    vulnName,
                    harm,
                    riskLevel != null ? riskLevel : "中危",
                    description,
                    suggestion
            );
        }

        // 关闭窗口
        stage.close();

        LogUtils.info(VulnAIGeneratorWindow.class, "已应用AI生成的漏洞信息");
    }

    // ==================== 公共方法 ====================

    /**
     * 显示窗口
     */
    public void show() {
        stage.show();
    }

    // ==================== 辅助方法 ====================

    /**
     * 显示错误提示
     * @param message 错误消息
     */
    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(stage);
        alert.showAndWait();
    }

    /**
     * 显示信息提示
     * @param message 信息消息
     */
    private void showInfoAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(stage);
        alert.showAndWait();
    }

    // ==================== 内部类 ====================

    /**
     * AI生成的漏洞信息结果
     */
    private static class VulnAIResult {
        private String harm;
        private String riskLevel;
        private String description;
        private String suggestion;

        public String getHarm() { return harm; }
        public void setHarm(String harm) { this.harm = harm; }
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getSuggestion() { return suggestion; }
        public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    }

    // ==================== 回调接口 ====================

    /**
     * AI生成结果回调接口
     */
    public interface VulnAIResultCallback {
        /**
         * 应用生成结果到编辑器
         * @param vulnName 漏洞名称
         * @param harm 漏洞危害
         * @param riskLevel 风险等级
         * @param description 漏洞描述
         * @param suggestion 修复建议
         */
        void onApplyResult(String vulnName, String harm, String riskLevel, String description, String suggestion);
    }
}
