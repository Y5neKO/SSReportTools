package com.y5neko.ssrtools.ui;

import com.y5neko.ssrtools.config.GlobalConfig;
import com.y5neko.ssrtools.utils.FileUtils;
import com.y5neko.ssrtools.utils.LogUtils;
import com.y5neko.ssrtools.utils.MiscUtils;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.y5neko.ssrtools.config.GlobalConfig.REPORT_TEMPLATE_DIR;

/**
 * 报告模板制作窗口
 * 用于快速制作和管理报告模板
 */
public class ReportTemplateMakerWindow {

    private Stage stage;
    private GridPane grid;

    // 基本信息字段
    private TextField templateNameField;
    private TextArea templateDescField;
    private ComboBox<String> templateTypeComboBox;

    // 模板内容字段
    private TextField customerNameField;
    private TextField contractorNameField;
    private TextField reportAuthorField;
    private TextField testerField;
    private TextField managerField;

    // 按钮样式
    private String primaryBtnStyle = "-fx-background-color: #4361ee; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 4px; -fx-padding: 6px 12px; -fx-font-size: 11px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(67, 97, 238, 0.2), 3, 0, 0, 1);";
    private String primaryBtnHover = "-fx-background-color: #3651de; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 4px; -fx-padding: 6px 12px; -fx-font-size: 11px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(67, 97, 238, 0.4), 4, 0, 0, 1);";

    private String secondaryBtnStyle = "-fx-background-color: #74b9ff; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 4px; -fx-padding: 6px 12px; -fx-font-size: 11px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(116, 185, 255, 0.2), 3, 0, 0, 1);";
    private String secondaryBtnHover = "-fx-background-color: #5ba3f5; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 4px; -fx-padding: 6px 12px; -fx-font-size: 11px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(116, 185, 255, 0.4), 4, 0, 0, 1);";

    private String successBtnStyle = "-fx-background-color: #26de81; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 4px; -fx-padding: 6px 12px; -fx-font-size: 11px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(38, 222, 129, 0.2), 3, 0, 0, 1);";
    private String successBtnHover = "-fx-background-color: #1eb980; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 4px; -fx-padding: 6px 12px; -fx-font-size: 11px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(38, 222, 129, 0.4), 4, 0, 0, 1);";

    public ReportTemplateMakerWindow() {
        setupUI();
        setupEventHandlers();
    }

    /**
     * 设置UI界面
     */
    private void setupUI() {
        grid = new GridPane();
        grid.setPadding(new Insets(12));
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setStyle("-fx-background-color: #f5f7fa; -fx-background-radius: 4px;");

        // 列约束
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.NEVER);
        col1.setMinWidth(80);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        int row = 0;

        // 标题
        Label titleLabel = new Label("报告模板制作");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #2d3436;");
        GridPane.setColumnSpan(titleLabel, 2);
        grid.add(titleLabel, 0, row++);

        // 分隔线
        Separator separator = new Separator();
        GridPane.setColumnSpan(separator, 2);
        grid.add(separator, 0, row++);

        // 基本信息
        Label basicInfoLabel = new Label("基本信息");
        basicInfoLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #2d3436;");
        GridPane.setColumnSpan(basicInfoLabel, 2);
        grid.add(basicInfoLabel, 0, row++);

        // 模板名称
        Label nameLabel = new Label("模板名称：");
        nameLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #2d3436;");
        grid.add(nameLabel, 0, row);
        templateNameField = new TextField();
        templateNameField.setPromptText("输入模板名称，如：标准金融行业报告模板");
        templateNameField.setStyle("-fx-font-size: 11px; -fx-padding: 4px 8px; -fx-border-radius: 4px; -fx-border-color: #dfe6e9; -fx-border-width: 1px; -fx-background-radius: 4px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-background-color: white;");
        grid.add(templateNameField, 1, row++);

        // 模板类型
        Label typeLabel = new Label("模板类型：");
        typeLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #2d3436;");
        grid.add(typeLabel, 0, row);
        templateTypeComboBox = new ComboBox<>();
        templateTypeComboBox.getItems().addAll("通用模板", "金融行业", "医疗行业", "政府机构", "教育机构", "电商企业", "其他");
        templateTypeComboBox.setValue("通用模板");
        templateTypeComboBox.setStyle("-fx-font-size: 11px; -fx-padding: 4px 8px; -fx-border-radius: 4px; -fx-border-color: #dfe6e9; -fx-border-width: 1px; -fx-background-radius: 4px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-background-color: white;");
        grid.add(templateTypeComboBox, 1, row++);

        // 模板描述
        Label descLabel = new Label("模板描述：");
        descLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #2d3436;");
        grid.add(descLabel, 0, row);
        templateDescField = new TextArea();
        templateDescField.setPromptText("输入模板描述，说明适用场景和特点");
        templateDescField.setStyle("-fx-font-size: 11px; -fx-padding: 4px 8px; -fx-border-radius: 4px; -fx-border-color: #dfe6e9; -fx-border-width: 1px; -fx-background-radius: 4px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-background-color: white;");
        templateDescField.setPrefRowCount(3);
        grid.add(templateDescField, 1, row++);

        // 预设内容
        Label presetLabel = new Label("预设内容");
        presetLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #2d3436;");
        GridPane.setColumnSpan(presetLabel, 2);
        grid.add(presetLabel, 0, row++);

        String fieldStyle = "-fx-font-size: 11px; -fx-padding: 4px 8px; -fx-border-radius: 4px; -fx-border-color: #dfe6e9; -fx-border-width: 1px; -fx-background-radius: 4px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-background-color: white;";
        String labelStyle = "-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #2d3436;";

        // 检测机构
        Label contractorLabel = new Label("检测机构：");
        contractorLabel.setStyle(labelStyle);
        grid.add(contractorLabel, 0, row);
        contractorNameField = new TextField();
        contractorNameField.setPromptText("默认检测机构名称");
        contractorNameField.setStyle(fieldStyle);
        grid.add(contractorNameField, 1, row++);

        // 报告作者
        Label authorLabel = new Label("报告作者：");
        authorLabel.setStyle(labelStyle);
        grid.add(authorLabel, 0, row);
        reportAuthorField = new TextField();
        reportAuthorField.setPromptText("默认报告作者");
        reportAuthorField.setStyle(fieldStyle);
        grid.add(reportAuthorField, 1, row++);

        // 测试人员
        Label testerLabel = new Label("测试人员：");
        testerLabel.setStyle(labelStyle);
        grid.add(testerLabel, 0, row);
        testerField = new TextField();
        testerField.setPromptText("默认测试人员");
        testerField.setStyle(fieldStyle);
        grid.add(testerField, 1, row++);

        // 项目经理
        Label managerLabel = new Label("项目经理：");
        managerLabel.setStyle(labelStyle);
        grid.add(managerLabel, 0, row);
        managerField = new TextField();
        managerField.setPromptText("默认项目经理");
        managerField.setStyle(fieldStyle);
        grid.add(managerField, 1, row++);

        // 底部按钮
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10, 0, 5, 0));

        Button saveButton = new Button("保存模板");
        saveButton.setStyle(successBtnStyle);
        saveButton.setOnMouseEntered(e -> saveButton.setStyle(successBtnHover));
        saveButton.setOnMouseExited(e -> saveButton.setStyle(successBtnStyle));

        Button clearButton = new Button("清空内容");
        clearButton.setStyle(secondaryBtnStyle);
        clearButton.setOnMouseEntered(e -> clearButton.setStyle(secondaryBtnHover));
        clearButton.setOnMouseExited(e -> clearButton.setStyle(secondaryBtnStyle));

        Button cancelButton = new Button("取消");
        cancelButton.setStyle(primaryBtnStyle);
        cancelButton.setOnMouseEntered(e -> cancelButton.setStyle(primaryBtnHover));
        cancelButton.setOnMouseExited(e -> cancelButton.setStyle(primaryBtnStyle));

        buttonBox.getChildren().addAll(saveButton, clearButton, cancelButton);
        GridPane.setColumnSpan(buttonBox, 2);
        grid.add(buttonBox, 0, row);
    }

    /**
     * 设置事件处理器
     */
    private void setupEventHandlers() {
        // 获取按钮引用
        Button saveButton = (Button) ((HBox) grid.getChildren().get(grid.getChildren().size() - 1)).getChildren().get(0);
        Button clearButton = (Button) ((HBox) grid.getChildren().get(grid.getChildren().size() - 1)).getChildren().get(1);
        Button cancelButton = (Button) ((HBox) grid.getChildren().get(grid.getChildren().size() - 1)).getChildren().get(2);

        // 保存按钮事件
        saveButton.setOnAction(e -> saveTemplate());

        // 清空按钮事件
        clearButton.setOnAction(e -> clearFields());

        // 取消按钮事件
        cancelButton.setOnAction(e -> stage.close());
    }

    /**
     * 保存模板
     */
    private void saveTemplate() {
        if (templateNameField.getText().trim().isEmpty()) {
            showAlert("错误", "请输入模板名称");
            return;
        }

        try {
            // 创建模板目录
            File templateDir = new File(MiscUtils.getAbsolutePath(REPORT_TEMPLATE_DIR));
            if (!templateDir.exists()) {
                templateDir.mkdirs();
            }

            // 构建模板数据
            String templateData = buildTemplateData();

            // 保存到文件
            String fileName = templateNameField.getText().trim() + ".json";
            File templateFile = new File(templateDir, fileName);
            FileUtils.overwrite(templateFile.getAbsolutePath(), templateData, StandardCharsets.UTF_8);

            showAlert("成功", "模板保存成功！");
            stage.close();

        } catch (IOException ex) {
            LogUtils.error(ReportTemplateMakerWindow.class, "保存模板失败：" + ex.getMessage());
            showAlert("错误", "保存模板失败：" + ex.getMessage());
        }
    }

    /**
     * 构建模板数据
     */
    private String buildTemplateData() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"templateName\": \"").append(escapeJson(templateNameField.getText().trim())).append("\",\n");
        sb.append("  \"templateType\": \"").append(escapeJson(templateTypeComboBox.getValue())).append("\",\n");
        sb.append("  \"templateDescription\": \"").append(escapeJson(templateDescField.getText().trim())).append("\",\n");
        sb.append("  \"createTime\": \"").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\",\n");
        sb.append("  \"defaultValues\": {\n");
        sb.append("    \"contractorName\": \"").append(escapeJson(contractorNameField.getText().trim())).append("\",\n");
        sb.append("    \"reportAuthor\": \"").append(escapeJson(reportAuthorField.getText().trim())).append("\",\n");
        sb.append("    \"testerName\": \"").append(escapeJson(testerField.getText().trim())).append("\",\n");
        sb.append("    \"managerName\": \"").append(escapeJson(managerField.getText().trim())).append("\"\n");
        sb.append("  }\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * 转义JSON字符串
     */
    private String escapeJson(String input) {
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    /**
     * 清空所有字段
     */
    private void clearFields() {
        templateNameField.clear();
        templateDescField.clear();
        contractorNameField.clear();
        reportAuthorField.clear();
        testerField.clear();
        managerField.clear();
        templateTypeComboBox.setValue("通用模板");
    }

    /**
     * 显示提示信息
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 获取主视图
     */
    public Parent getView() {
        return grid;
    }

    /**
     * 显示窗口
     */
    public void show() {
        stage = new Stage();
        stage.setTitle("报告模板制作");
        stage.setScene(new Scene(getView(), 500, 600));
        stage.setResizable(true);
        stage.setMinWidth(450);
        stage.setMinHeight(500);
        stage.showAndWait();
    }
}