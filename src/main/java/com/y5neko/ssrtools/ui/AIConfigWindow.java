package com.y5neko.ssrtools.ui;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.y5neko.ssrtools.config.GlobalConfig;
import com.y5neko.ssrtools.models.aiconfig.AIEndpointConfig;
import com.y5neko.ssrtools.services.AIService;
import com.y5neko.ssrtools.utils.LogUtils;
import com.y5neko.ssrtools.utils.MiscUtils;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.*;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * AI配置管理窗口
 * 用于管理OpenAI兼容格式的API端点配置
 */
public class AIConfigWindow {
    private Stage stage;

    private TableView<AIEndpointConfig> table;
    private final ObservableList<AIEndpointConfig> masterData = FXCollections.observableArrayList();

    // 表单输入控件
    private ComboBox<PresetEndpoint> presetComboBox;  // 预设端点下拉框
    private TextField nameField;
    private TextField endpointField;
    private TextField apiKeyField;
    private TextField modelField;
    private TextField providerField;
    private TextField maxTokensField;
    private TextField temperatureField;
    private TextField connectTimeoutField;  // 连接超时（秒）
    private TextField readTimeoutField;     // 读取超时（秒）

    // 按钮
    private Button btnNew;
    private Button btnSave;
    private Button btnDelete;
    private Button btnImport;
    private Button btnExport;
    private Button btnTest;

    /**
     * 预设端点类
     */
    private static class PresetEndpoint {
        private final String displayName;
        private final String name;
        private final String endpoint;
        private final String model;
        private final String provider;
        private final Integer maxTokens;
        private final Double temperature;

        public PresetEndpoint(String displayName, String name, String endpoint, String model, String provider, Integer maxTokens, Double temperature) {
            this.displayName = displayName;
            this.name = name;
            this.endpoint = endpoint;
            this.model = model;
            this.provider = provider;
            this.maxTokens = maxTokens;
            this.temperature = temperature;
        }

        public String getDisplayName() { return displayName; }
        public String getName() { return name; }
        public String getEndpoint() { return endpoint; }
        public String getModel() { return model; }
        public String getProvider() { return provider; }
        public Integer getMaxTokens() { return maxTokens; }
        public Double getTemperature() { return temperature; }

        @Override
        public String toString() { return displayName; }
    }

    /**
     * 单选按钮单元格
     * 用于在表格中显示单选按钮，实现单选启用功能
     */
    private class RadioButtonCell extends TableCell<AIEndpointConfig, Boolean> {
        private final RadioButton radioButton;
        private final TableView<AIEndpointConfig> table;

        public RadioButtonCell(TableView<AIEndpointConfig> table) {
            this.table = table;
            this.radioButton = new RadioButton();
            radioButton.setSelected(false);

            // 单选按钮点击事件 - 立即保存启用状态
            radioButton.setOnAction(e -> {
                if (radioButton.isSelected()) {
                    // 取消其他所有行的选中状态
                    ObservableList<AIEndpointConfig> items = table.getItems();
                    for (AIEndpointConfig config : items) {
                        config.setEnabled(false);
                    }
                    // 启用当前行
                    AIEndpointConfig currentConfig = getTableView().getItems().get(getIndex());
                    currentConfig.setEnabled(true);
                    // 刷新表格显示
                    table.refresh();
                    // 立即保存到配置文件
                    saveConfigToFile();
                }
            });

            // 居中显示
            setAlignment(Pos.CENTER);
        }

        @Override
        protected void updateItem(Boolean enabled, boolean empty) {
            super.updateItem(enabled, empty);

            if (empty || enabled == null) {
                setGraphic(null);
            } else {
                radioButton.setSelected(enabled);
                setGraphic(radioButton);
            }
        }
    }

    /**
     * 构造函数
     */
    public AIConfigWindow() {
        initControls();
        stage = new Stage();
    }

    /**
     * 显示AI配置管理窗口
     */
    public void show() {
        stage.setTitle("AI配置管理器");
        BorderPane root = layoutUI();
        bindEvents(stage);
        stage.setScene(new Scene(root, 1000, 650));
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
        loadDefaultConfig();
    }

    /**
     * 初始化控件
     */
    private void initControls() {
        // 初始化表格
        table = new TableView<>();
        table.setPlaceholder(new Label("暂无AI配置"));
        table.setStyle("-fx-background-color: white; -fx-border-color: #dfe6e9; -fx-border-radius: 8px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        // 初始化预设端点下拉框
        presetComboBox = new ComboBox<>();
        presetComboBox.setPromptText("选择预设端点或自定义");
        presetComboBox.setStyle("-fx-font-size: 11px; -fx-padding: 2px 6px; -fx-border-radius: 6px; -fx-border-color: #dfe6e9; -fx-border-width: 1px; -fx-background-radius: 6px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        // 添加预设端点选项
        ObservableList<PresetEndpoint> presets = FXCollections.observableArrayList(
                new PresetEndpoint("OpenAI官方", "OpenAI官方", "https://api.openai.com/v1/chat/completions", "gpt-3.5-turbo", "OpenAI", 2000, 0.7),
                new PresetEndpoint("OpenAI GPT-4", "OpenAI GPT-4", "https://api.openai.com/v1/chat/completions", "gpt-4", "OpenAI", 2000, 0.7),
                new PresetEndpoint("Azure OpenAI", "Azure OpenAI", "https://your-resource.openai.azure.com/openai/deployments/your-deployment/chat/completions?api-version=2023-05-15", "gpt-35-turbo", "Azure", 2000, 0.7),
                new PresetEndpoint("Anthropic Claude", "Anthropic Claude", "https://api.anthropic.com/v1/messages", "claude-3-sonnet-20240229", "Anthropic", 2000, 0.7),
                new PresetEndpoint("DeepSeek", "DeepSeek", "https://api.deepseek.com/v1/chat/completions", "deepseek-chat", "DeepSeek", 2000, 0.7),
                new PresetEndpoint("通义千问", "通义千问", "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions", "qwen-turbo", "阿里云", 2000, 0.7),
                new PresetEndpoint("文心一言", "文心一言", "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat", "ernie-bot-turbo", "百度", 2000, 0.7),
                new PresetEndpoint("智谱AI GLM", "智谱AI GLM", "https://open.bigmodel.cn/api/paas/v4/chat/completions", "glm-4", "智谱AI", 2000, 0.7),
                new PresetEndpoint("Kimi", "Kimi", "https://api.moonshot.cn/v1/chat/completions", "moonshot-v1-8k", "Moonshot", 2000, 0.7),
                new PresetEndpoint("自定义端点", "自定义", "", "", "自定义", 2000, 0.7)
        );
        presetComboBox.setItems(presets);

        // 监听选择变化，自动填充表单
        presetComboBox.setOnAction(e -> handlePresetSelection());

        // 初始化表单输入控件
        nameField = new TextField();
        nameField.setPromptText("请输入配置名称");
        nameField.setStyle("-fx-font-size: 12px; -fx-padding: 6px 8px; -fx-border-radius: 6px; -fx-border-color: #dfe6e9; -fx-border-width: 1px; -fx-background-radius: 6px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        endpointField = new TextField();
        endpointField.setPromptText("请输入API端点URL，如: https://api.openai.com/v1/chat/completions");
        endpointField.setStyle("-fx-font-size: 12px; -fx-padding: 6px 8px; -fx-border-radius: 6px; -fx-border-color: #dfe6e9; -fx-border-width: 1px; -fx-background-radius: 6px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        apiKeyField = new TextField();
        apiKeyField.setPromptText("请输入API密钥");
        apiKeyField.setStyle("-fx-font-size: 12px; -fx-padding: 6px 8px; -fx-border-radius: 6px; -fx-border-color: #dfe6e9; -fx-border-width: 1px; -fx-background-radius: 6px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        modelField = new TextField();
        modelField.setPromptText("请输入模型名称，如: gpt-3.5-turbo");
        modelField.setStyle("-fx-font-size: 12px; -fx-padding: 6px 8px; -fx-border-radius: 6px; -fx-border-color: #dfe6e9; -fx-border-width: 1px; -fx-background-radius: 6px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        providerField = new TextField();
        providerField.setPromptText("请输入提供商名称");
        providerField.setStyle("-fx-font-size: 12px; -fx-padding: 6px 8px; -fx-border-radius: 6px; -fx-border-color: #dfe6e9; -fx-border-width: 1px; -fx-background-radius: 6px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        maxTokensField = new TextField();
        maxTokensField.setPromptText("2000");
        maxTokensField.setStyle("-fx-font-size: 12px; -fx-padding: 6px 8px; -fx-border-radius: 6px; -fx-border-color: #dfe6e9; -fx-border-width: 1px; -fx-background-radius: 6px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        temperatureField = new TextField();
        temperatureField.setPromptText("0.7");
        temperatureField.setStyle("-fx-font-size: 12px; -fx-padding: 6px 8px; -fx-border-radius: 6px; -fx-border-color: #dfe6e9; -fx-border-width: 1px; -fx-background-radius: 6px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        connectTimeoutField = new TextField();
        connectTimeoutField.setPromptText("30");
        connectTimeoutField.setStyle("-fx-font-size: 12px; -fx-padding: 6px 8px; -fx-border-radius: 6px; -fx-border-color: #dfe6e9; -fx-border-width: 1px; -fx-background-radius: 6px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        readTimeoutField = new TextField();
        readTimeoutField.setPromptText("60");
        readTimeoutField.setStyle("-fx-font-size: 12px; -fx-padding: 6px 8px; -fx-border-radius: 6px; -fx-border-color: #dfe6e9; -fx-border-width: 1px; -fx-background-radius: 6px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        // 初始化按钮
        btnNew = new Button("新增配置");
        btnSave = new Button("保存到列表");
        btnDelete = new Button("删除选中");
        btnImport = new Button("导入配置");
        btnExport = new Button("导出配置");
        btnTest = new Button("测试连接");

        // 应用按钮样式
        applyButtonStyles();
    }

    /**
     * 应用按钮样式
     */
    private void applyButtonStyles() {
        // 主要按钮样式
        String primaryBtnStyle = "-fx-background-color: #4361ee; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 5px; -fx-padding: 6px 12px; -fx-font-size: 11px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(67, 97, 238, 0.2), 3, 0, 0, 1);";
        String primaryBtnHover = "-fx-background-color: #3651de; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 5px; -fx-padding: 6px 12px; -fx-font-size: 11px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(67, 97, 238, 0.4), 5, 0, 0, 2);";

        // 成功按钮样式
        String successBtnStyle = "-fx-background-color: #26de81; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 5px; -fx-padding: 6px 12px; -fx-font-size: 11px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(38, 222, 129, 0.2), 3, 0, 0, 1);";
        String successBtnHover = "-fx-background-color: #1eb980; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 5px; -fx-padding: 6px 12px; -fx-font-size: 11px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(38, 222, 129, 0.4), 5, 0, 0, 2);";

        // 危险按钮样式
        String dangerBtnStyle = "-fx-background-color: #f53e57; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 5px; -fx-padding: 6px 12px; -fx-font-size: 11px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(245, 62, 87, 0.2), 3, 0, 0, 1);";
        String dangerBtnHover = "-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 5px; -fx-padding: 6px 12px; -fx-font-size: 11px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(245, 62, 87, 0.4), 5, 0, 0, 2);";

        // 次要按钮样式
        String secondaryBtnStyle = "-fx-background-color: #74b9ff; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 5px; -fx-padding: 6px 12px; -fx-font-size: 11px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(116, 185, 255, 0.2), 3, 0, 0, 1);";
        String secondaryBtnHover = "-fx-background-color: #5ba3f5; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 5px; -fx-padding: 6px 12px; -fx-font-size: 11px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(116, 185, 255, 0.4), 5, 0, 0, 2);";

        // 警告按钮样式
        String warningBtnStyle = "-fx-background-color: #ff9f43; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 5px; -fx-padding: 6px 12px; -fx-font-size: 11px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(255, 159, 67, 0.2), 3, 0, 0, 1);";
        String warningBtnHover = "-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 5px; -fx-padding: 6px 12px; -fx-font-size: 11px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(255, 159, 67, 0.4), 5, 0, 0, 2);";

        // 应用到按钮
        btnNew.setStyle(primaryBtnStyle);
        btnSave.setStyle(successBtnStyle);
        btnDelete.setStyle(dangerBtnStyle);
        btnImport.setStyle(secondaryBtnStyle);
        btnExport.setStyle(secondaryBtnStyle);
        btnTest.setStyle(warningBtnStyle);

        // 添加悬停效果
        addHoverEffect(btnNew, primaryBtnStyle, primaryBtnHover);
        addHoverEffect(btnSave, successBtnStyle, successBtnHover);
        addHoverEffect(btnDelete, dangerBtnStyle, dangerBtnHover);
        addHoverEffect(btnImport, secondaryBtnStyle, secondaryBtnHover);
        addHoverEffect(btnExport, secondaryBtnStyle, secondaryBtnHover);
        addHoverEffect(btnTest, warningBtnStyle, warningBtnHover);
    }

    /**
     * 为按钮添加悬停效果
     */
    private void addHoverEffect(Button button, String normalStyle, String hoverStyle) {
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(normalStyle));
    }

    /**
     * 布局UI
     * @return 根节点
     */
    private BorderPane layoutUI() {
        // 表格列配置
        TableColumn<AIEndpointConfig, String> nameCol = new TableColumn<>("配置名称");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(180);
        nameCol.setMinWidth(150);
        nameCol.setMaxWidth(250);
        nameCol.setStyle("-fx-font-size: 14px; -fx-font-weight: 600;");
        nameCol.setResizable(true);

        TableColumn<AIEndpointConfig, String> providerCol = new TableColumn<>("提供商");
        providerCol.setCellValueFactory(new PropertyValueFactory<>("provider"));
        providerCol.setPrefWidth(100);
        providerCol.setMinWidth(80);
        providerCol.setMaxWidth(120);
        providerCol.setStyle("-fx-font-size: 14px; -fx-font-weight: 600;");
        providerCol.setResizable(true);

        TableColumn<AIEndpointConfig, String> endpointCol = new TableColumn<>("端点地址");
        endpointCol.setCellValueFactory(cell -> {
            String endpoint = cell.getValue().getEndpoint();
            if (endpoint == null) endpoint = "";
            // 显示前40个字符
            String preview = endpoint.length() > 40 ? endpoint.substring(0, 37) + "..." : endpoint;
            return new SimpleStringProperty(preview);
        });
        endpointCol.setPrefWidth(300);
        endpointCol.setMinWidth(250);
        endpointCol.setStyle("-fx-font-size: 14px; -fx-font-weight: 600;");
        endpointCol.setResizable(true);

        // 单选按钮列
        TableColumn<AIEndpointConfig, Boolean> radioCol = new TableColumn<>("启用");
        radioCol.setCellValueFactory(cell -> new SimpleBooleanProperty(cell.getValue().isEnabled()));
        radioCol.setCellFactory(col -> new RadioButtonCell(table));
        radioCol.setPrefWidth(60);
        radioCol.setMinWidth(50);
        radioCol.setMaxWidth(80);
        radioCol.setStyle("-fx-font-size: 14px; -fx-font-weight: 600;");
        radioCol.setResizable(true);

        table.getColumns().setAll(nameCol, providerCol, endpointCol, radioCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // 绑定数据到表格
        table.setItems(masterData);

        // 创建左侧表格区域
        Label tableTitle = new Label("AI端点列表");
        tableTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #2d3436;");

        VBox tableBox = new VBox(5);
        tableBox.setPadding(new Insets(10));
        tableBox.setStyle("-fx-background-color: #f8f9fa; -fx-border-radius: 8px; -fx-background-radius: 8px;");
        tableBox.getChildren().addAll(tableTitle, table);

        // 设置表格垂直扩展
        VBox.setVgrow(table, Priority.ALWAYS);

        // 创建右侧表单区域
        Label formTitle = new Label("配置详情编辑");
        formTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #2d3436;");

        // 创建字段标签样式
        String labelStyle = "-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #636e72;";

        Label nameLabel = new Label("配置名称");
        nameLabel.setStyle(labelStyle);

        Label endpointLabel = new Label("端点地址");
        endpointLabel.setStyle(labelStyle);

        Label apiKeyLabel = new Label("API密钥");
        apiKeyLabel.setStyle(labelStyle);

        Label modelLabel = new Label("模型名称");
        modelLabel.setStyle(labelStyle);

        Label providerLabel = new Label("提供商");
        providerLabel.setStyle(labelStyle);

        // 参数字段：label和输入框分行显示
        Label maxTokensLabel = new Label("最大Token");
        maxTokensLabel.setStyle(labelStyle);

        Label temperatureLabel = new Label("温度");
        temperatureLabel.setStyle(labelStyle);

        Label connectTimeoutLabel = new Label("连接超时(秒)");
        connectTimeoutLabel.setStyle(labelStyle);

        Label readTimeoutLabel = new Label("读取超时(秒)");
        readTimeoutLabel.setStyle(labelStyle);

        // 创建主要操作按钮行
        HBox primaryButtons = new HBox(6, btnNew, btnSave, btnDelete);
        primaryButtons.setAlignment(Pos.CENTER);
        primaryButtons.setPadding(new Insets(6, 0, 4, 0));
        primaryButtons.setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        // 创建文件操作和测试按钮行
        HBox secondaryButtons = new HBox(6, btnImport, btnExport, btnTest);
        secondaryButtons.setAlignment(Pos.CENTER);
        secondaryButtons.setPadding(new Insets(0, 0, 6, 0));
        secondaryButtons.setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        // 创建表单容器 - 使用更紧凑的间距
        VBox form = new VBox(5);
        form.setPadding(new Insets(10));
        form.setStyle("-fx-background-color: white; -fx-border-color: #dfe6e9; -fx-border-radius: 10px; -fx-background-radius: 10px;");
        form.getChildren().addAll(
                formTitle,
                presetComboBox,
                nameLabel, nameField,
                endpointLabel, endpointField,
                apiKeyLabel, apiKeyField,
                modelLabel, modelField,
                providerLabel, providerField,
                maxTokensLabel, maxTokensField,
                temperatureLabel, temperatureField,
                connectTimeoutLabel, connectTimeoutField,
                readTimeoutLabel, readTimeoutField,
                primaryButtons,
                secondaryButtons
        );

        form.setPrefWidth(400);
        form.setMaxWidth(420);

        // 设置根容器
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #f5f7fa;");

        // 创建左右分割容器
        HBox mainContent = new HBox(12);
        mainContent.getChildren().addAll(tableBox, form);
        HBox.setHgrow(tableBox, Priority.ALWAYS);
        HBox.setHgrow(form, Priority.NEVER);

        root.setCenter(mainContent);
        return root;
    }

    /**
     * 绑定事件
     * @param stage 窗口
     */
    private void bindEvents(Stage stage) {
        // 表格交互
        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1) loadSelectedToForm();
        });
        table.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) loadSelectedToForm();
        });

        // 按钮事件
        btnNew.setOnAction(e -> onNew());
        btnSave.setOnAction(e -> onSave());
        btnDelete.setOnAction(e -> onDelete());
        btnImport.setOnAction(e -> onImport(stage));
        btnExport.setOnAction(e -> onExport(stage));
        btnTest.setOnAction(e -> onTest());
    }

    /**
     * 加载选中配置到表单
     */
    private void loadSelectedToForm() {
        AIEndpointConfig selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            nameField.setText(selected.getName());
            endpointField.setText(selected.getEndpoint());
            apiKeyField.setText(selected.getApiKey());
            modelField.setText(selected.getModel());
            providerField.setText(selected.getProvider());
            maxTokensField.setText(selected.getMaxTokens() != null ? selected.getMaxTokens().toString() : "2000");
            temperatureField.setText(selected.getTemperature() != null ? selected.getTemperature().toString() : "0.7");
            connectTimeoutField.setText(selected.getConnectTimeout() != null ? selected.getConnectTimeout().toString() : "30");
            readTimeoutField.setText(selected.getReadTimeout() != null ? selected.getReadTimeout().toString() : "60");
        }
    }

    /**
     * 新增配置
     */
    private void onNew() {
        clearForm();
        nameField.requestFocus();
    }

    /**
     * 保存配置到列表
     */
    private void onSave() {
        // 验证表单
        String name = nameField.getText().trim();
        String endpoint = endpointField.getText().trim();
        String model = modelField.getText().trim();

        // 必填字段验证
        if (name.isEmpty()) {
            showErrorAlert("请输入配置名称");
            return;
        }
        if (endpoint.isEmpty()) {
            showErrorAlert("请输入端点地址");
            return;
        }
        if (model.isEmpty()) {
            showErrorAlert("请输入模型名称");
            return;
        }

        // URL格式验证
        if (!isValidUrl(endpoint)) {
            showErrorAlert("端点地址格式不正确，请输入有效的URL");
            return;
        }

        // 解析参数
        Integer maxTokens = 2000;
        try {
            if (!maxTokensField.getText().trim().isEmpty()) {
                maxTokens = Integer.parseInt(maxTokensField.getText().trim());
            }
        } catch (NumberFormatException e) {
            showErrorAlert("最大Token必须是数字");
            return;
        }

        Double temperature = 0.7;
        try {
            if (!temperatureField.getText().trim().isEmpty()) {
                temperature = Double.parseDouble(temperatureField.getText().trim());
            }
        } catch (NumberFormatException e) {
            showErrorAlert("温度参数必须是数字");
            return;
        }

        // 解析超时参数
        Integer connectTimeout = 30;
        try {
            if (!connectTimeoutField.getText().trim().isEmpty()) {
                connectTimeout = Integer.parseInt(connectTimeoutField.getText().trim());
                if (connectTimeout < 1 || connectTimeout > 300) {
                    showErrorAlert("连接超时必须在1-300秒之间");
                    return;
                }
            }
        } catch (NumberFormatException e) {
            showErrorAlert("连接超时必须是数字");
            return;
        }

        Integer readTimeout = 60;
        try {
            if (!readTimeoutField.getText().trim().isEmpty()) {
                readTimeout = Integer.parseInt(readTimeoutField.getText().trim());
                if (readTimeout < 1 || readTimeout > 600) {
                    showErrorAlert("读取超时必须在1-600秒之间");
                    return;
                }
            }
        } catch (NumberFormatException e) {
            showErrorAlert("读取超时必须是数字");
            return;
        }

        // 获取当前选中的配置（用于判断是否为编辑模式）
        AIEndpointConfig selected = table.getSelectionModel().getSelectedItem();

        // 创建配置对象
        AIEndpointConfig config = new AIEndpointConfig();
        config.setName(name);
        config.setEndpoint(endpoint);
        config.setApiKey(apiKeyField.getText().trim());
        config.setModel(model);
        config.setProvider(providerField.getText().trim());
        config.setMaxTokens(maxTokens);
        config.setTemperature(temperature);
        config.setConnectTimeout(connectTimeout);
        config.setReadTimeout(readTimeout);

        // 编辑模式：保持原有启用状态；新增模式：默认不启用
        boolean isEnabled;
        if (selected != null && selected.getName().equals(name)) {
            isEnabled = selected.isEnabled();
        } else {
            isEnabled = false;  // 新增配置默认不启用，让用户在表格中选择
        }
        config.setEnabled(isEnabled);

        // 如果是编辑模式且当前配置被启用，保持单选状态（已经由表格控制）
        // 如果是新增模式且设置为启用，禁用其他所有配置
        if (isEnabled && (selected == null || !selected.getName().equals(name))) {
            for (AIEndpointConfig c : masterData) {
                c.setEnabled(false);
            }
        }

        // 检查是否是编辑模式
        if (selected != null && selected.getName().equals(name)) {
            // 编辑模式：更新现有配置
            int index = masterData.indexOf(selected);
            masterData.set(index, config);
            LogUtils.info(AIConfigWindow.class, "更新AI配置: " + name);
        } else {
            // 新增模式：检查名称是否重复
            boolean exists = masterData.stream().anyMatch(c -> c.getName().equals(name));
            if (exists) {
                showErrorAlert("配置名称已存在，请使用其他名称");
                return;
            }
            masterData.add(config);
            LogUtils.info(AIConfigWindow.class, "新增AI配置: " + name);
        }

        // 刷新表格
        table.refresh();

        // 自动保存到文件
        saveConfigToFile();
    }

    /**
     * 删除选中配置
     */
    private void onDelete() {
        AIEndpointConfig selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showErrorAlert("请先选择要删除的配置");
            return;
        }

        // 确认对话框
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText("确定要删除配置 \"" + selected.getName() + "\" 吗？");
        alert.setContentText("此操作无法撤销。");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                masterData.remove(selected);
                clearForm();
                saveConfigToFile();
                LogUtils.info(AIConfigWindow.class, "删除AI配置: " + selected.getName());
            }
        });
    }

    /**
     * 导入配置文件
     */
    private void onImport(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择要导入的配置文件");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON文件 (*.json)", "*.json")
        );

        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try {
                String content = new String(Files.readAllBytes(file.toPath()));
                JSONObject jsonObject = JSON.parseObject(content);
                List<AIEndpointConfig> endpoints = jsonObject.getList("endpoints", AIEndpointConfig.class);

                if (endpoints != null && !endpoints.isEmpty()) {
                    masterData.clear();
                    masterData.addAll(endpoints);

                    // 确保只有一个配置是启用状态
                    ensureSingleEnabledConfig();

                    table.refresh();
                    LogUtils.info(AIConfigWindow.class, "导入AI配置成功: " + file.getAbsolutePath());
                    showInfoAlert("导入成功，共导入 " + endpoints.size() + " 个配置");
                } else {
                    showErrorAlert("配置文件格式错误或为空");
                }
            } catch (IOException e) {
                LogUtils.error(AIConfigWindow.class, "导入AI配置失败", e);
                showErrorAlert("导入配置文件失败: " + e.getMessage());
            }
        }
    }

    /**
     * 导出配置文件
     */
    private void onExport(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("保存配置文件");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON文件 (*.json)", "*.json")
        );
        fileChooser.setInitialFileName("ai_endpoints.json");

        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("endpoints", masterData);

                String jsonStr = JSON.toJSONString(jsonObject, com.alibaba.fastjson2.JSONWriter.Feature.PrettyFormat);

                Files.write(file.toPath(), jsonStr.getBytes());
                LogUtils.info(AIConfigWindow.class, "导出AI配置成功: " + file.getAbsolutePath());
                showInfoAlert("导出成功");
            } catch (IOException e) {
                LogUtils.error(AIConfigWindow.class, "导出AI配置失败", e);
                showErrorAlert("导出配置文件失败: " + e.getMessage());
            }
        }
    }

    /**
     * 测试连接
     */
    private void onTest() {
        // 验证表单
        String endpoint = endpointField.getText().trim();
        String apiKey = apiKeyField.getText().trim();
        String model = modelField.getText().trim();

        if (endpoint.isEmpty()) {
            showErrorAlert("请输入端点地址");
            return;
        }
        if (apiKey.isEmpty()) {
            showErrorAlert("请输入API密钥");
            return;
        }
        if (model.isEmpty()) {
            showErrorAlert("请输入模型名称");
            return;
        }

        // URL格式验证
        if (!isValidUrl(endpoint)) {
            showErrorAlert("端点地址格式不正确，请输入有效的URL");
            return;
        }

        // 解析参数
        Integer maxTokens = 2000;
        try {
            if (!maxTokensField.getText().trim().isEmpty()) {
                maxTokens = Integer.parseInt(maxTokensField.getText().trim());
            }
        } catch (NumberFormatException e) {
            showErrorAlert("最大Token必须是数字");
            return;
        }

        Double temperature = 0.7;
        try {
            if (!temperatureField.getText().trim().isEmpty()) {
                temperature = Double.parseDouble(temperatureField.getText().trim());
            }
        } catch (NumberFormatException e) {
            showErrorAlert("温度参数必须是数字");
            return;
        }

        // 解析超时参数
        Integer connectTimeout = 30;
        try {
            if (!connectTimeoutField.getText().trim().isEmpty()) {
                connectTimeout = Integer.parseInt(connectTimeoutField.getText().trim());
            }
        } catch (NumberFormatException e) {
            // 使用默认值
        }

        Integer readTimeout = 60;
        try {
            if (!readTimeoutField.getText().trim().isEmpty()) {
                readTimeout = Integer.parseInt(readTimeoutField.getText().trim());
            }
        } catch (NumberFormatException e) {
            // 使用默认值
        }

        // 创建测试配置
        AIEndpointConfig testConfig = new AIEndpointConfig();
        testConfig.setEndpoint(endpoint);
        testConfig.setApiKey(apiKey);
        testConfig.setModel(model);
        testConfig.setMaxTokens(maxTokens);
        testConfig.setTemperature(temperature);
        testConfig.setConnectTimeout(connectTimeout);
        testConfig.setReadTimeout(readTimeout);

        // 保存原始按钮状态
        String originalText = btnTest.getText();
        boolean originalDisabled = btnTest.isDisabled();

        // 更新按钮状态为"测试中..."
        btnTest.setText("测试中...");
        btnTest.setDisable(true);

        // 立即在后台线程执行测试
        new Thread(() -> {
            String result = AIService.testConnection(testConfig);

            // 在UI线程显示结果并恢复按钮状态
            javafx.application.Platform.runLater(() -> {
                // 恢复按钮状态
                btnTest.setText(originalText);
                btnTest.setDisable(originalDisabled);

                // 显示测试结果
                if (result.startsWith("连接成功")) {
                    showInfoAlert(result);
                    LogUtils.info(AIConfigWindow.class, "AI连接测试成功: " + nameField.getText().trim());
                } else {
                    showErrorAlert(result);
                    LogUtils.error(AIConfigWindow.class, "AI连接测试失败: " + result);
                }
            });
        }).start();
    }

    /**
     * 清空表单
     */
    private void clearForm() {
        presetComboBox.setValue(null);
        nameField.clear();
        endpointField.clear();
        apiKeyField.clear();
        modelField.clear();
        providerField.clear();
        maxTokensField.setText("2000");
        temperatureField.setText("0.7");
        connectTimeoutField.setText("30");
        readTimeoutField.setText("60");
        table.getSelectionModel().clearSelection();
    }

    /**
     * 处理预设端点选择
     */
    private void handlePresetSelection() {
        PresetEndpoint selected = presetComboBox.getValue();
        if (selected != null) {
            // 自动填充表单
            nameField.setText(selected.getName());
            endpointField.setText(selected.getEndpoint());
            modelField.setText(selected.getModel());
            providerField.setText(selected.getProvider());
            maxTokensField.setText(String.valueOf(selected.getMaxTokens()));
            temperatureField.setText(String.valueOf(selected.getTemperature()));

            // 如果是自定义端点，清空端点地址让用户手动输入
            if ("自定义".equals(selected.getName())) {
                endpointField.clear();
                endpointField.setPromptText("请输入自定义API端点URL");
            }
        }
    }

    /**
     * 验证URL格式
     */
    private boolean isValidUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            url.toURI();
            return true;
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }
    }

    /**
     * 确保只有一个配置是启用状态
     * 如果有多个启用的配置，只保留第一个，禁用其他的
     */
    private void ensureSingleEnabledConfig() {
        boolean foundEnabled = false;
        for (AIEndpointConfig config : masterData) {
            if (config.isEnabled()) {
                if (foundEnabled) {
                    // 已经找到一个启用的配置，禁用这个
                    config.setEnabled(false);
                } else {
                    // 这是第一个启用的配置
                    foundEnabled = true;
                }
            }
        }
    }

    /**
     * 加载默认配置
     */
    private void loadDefaultConfig() {
        String configPath = MiscUtils.getAbsolutePath(GlobalConfig.AI_CONFIG_FILE);
        File file = new File(configPath);

        if (!file.exists()) {
            LogUtils.info(AIConfigWindow.class, "AI配置文件不存在，将使用默认配置: " + configPath);
            return;
        }

        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            JSONObject jsonObject = JSON.parseObject(content);
            List<AIEndpointConfig> endpoints = jsonObject.getList("endpoints", AIEndpointConfig.class);

            if (endpoints != null && !endpoints.isEmpty()) {
                masterData.clear();
                masterData.addAll(endpoints);

                // 确保只有一个配置是启用状态
                ensureSingleEnabledConfig();

                table.refresh();
                LogUtils.info(AIConfigWindow.class, "加载AI配置成功，共 " + endpoints.size() + " 个配置");
            }
        } catch (IOException e) {
            LogUtils.error(AIConfigWindow.class, "加载AI配置失败", e);
        }
    }

    /**
     * 保存配置到文件
     */
    private void saveConfigToFile() {
        String configPath = MiscUtils.getAbsolutePath(GlobalConfig.AI_CONFIG_FILE);
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("endpoints", masterData);

            String jsonStr = JSON.toJSONString(jsonObject, com.alibaba.fastjson2.JSONWriter.Feature.PrettyFormat);

            // 确保目录存在
            File file = new File(configPath);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            Files.write(file.toPath(), jsonStr.getBytes());
            LogUtils.info(AIConfigWindow.class, "保存AI配置成功");
        } catch (IOException e) {
            LogUtils.error(AIConfigWindow.class, "保存AI配置失败", e);
        }
    }

    /**
     * 显示错误提示
     */
    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 显示信息提示
     */
    private void showInfoAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
