package com.y5neko.ssrtools.ui;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.y5neko.ssrtools.models.docdata.ReportData;
import com.y5neko.ssrtools.models.docdata.SystemInfo;
import com.y5neko.ssrtools.models.docdata.Unit;
import com.y5neko.ssrtools.models.docdata.Vulnerability;
import com.y5neko.ssrtools.object.DocObj;
import com.y5neko.ssrtools.utils.DocUtils;
import com.y5neko.ssrtools.utils.FileUtils;
import com.y5neko.ssrtools.utils.LogUtils;
import com.y5neko.ssrtools.utils.MiscUtils;
import com.y5neko.ssrtools.config.GlobalConfig;
import javafx.beans.binding.Bindings;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Optional;

import static com.y5neko.ssrtools.config.GlobalConfig.COMPANY_TEMPLATE_DIR;
import static com.y5neko.ssrtools.config.GlobalConfig.VULN_TREE_PATH;

/**
 * 主窗口
 */
public class MainWindow {
    // 主界面组件相关成员变量
    private final ScrollPane scrollPane;
    private final GridPane grid;

    public TextField clientNameField;
    public RadioButton rbInitialTest;
    public RadioButton rbRetestTest;
    public TextField contractorNameField;
    public TextField testDateField;
    public TextField reportYearField;
    public TextField reportMonthField;
    public TextField reportDayField;
    public TextField reportAuthorField;
    public TextField testerField;
    public TextField managerField;
    public TextField highVulnField;
    public TextField midVulnField;
    public TextField lowVulnField;
    public TextField totalVulnField;

    // 模板相关成员变量
    private ComboBox<String> templateComboBox; // 主界面的报告模板选择
    private ComboBox<String> customerTemplateComboBox; // 客户模板选择
    private TextField templateNameField;
    private Button saveTemplateButton;
    private VBox mainVBox;
    private Button deleteTemplateButton;
    private Button reportTemplateBtn;

    // 当前使用的模板路径
    private String currentTemplatePath;

    // 漏洞库编辑按钮
    private VulnEditorWindow vuLnEditorWindow;

    /**
     * 构造函数
     */
    public MainWindow() {
        // 初始化当前模板路径为空，使用默认模板
        this.currentTemplatePath = null;

        grid = new GridPane();
        grid.setPadding(new Insets(8));
        grid.setHgap(8);
        grid.setVgap(7);
        grid.setStyle("-fx-background-color: #f5f7fa; -fx-background-radius: 4px;");

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.NEVER);
        col1.setMinWidth(Region.USE_PREF_SIZE);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        int row = 0;

        // 甲方爸爸名称
        Label clientLabel = new Label("客户名称：");
        clientLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #2d3436;");
        grid.add(clientLabel, 0, row);
        clientNameField = new TextField();
        clientNameField.setPromptText("填写客户公司名称，显示在报告顶部、声明及文件名等位置");
        clientNameField.setStyle("-fx-font-size: 11px; -fx-padding: 4px 8px; -fx-border-radius: 4px; -fx-border-color: #dfe6e9; -fx-border-width: 1px; -fx-background-radius: 4px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-background-color: white;");
        grid.add(clientNameField, 1, row++);

        // 是否初测
        Label testTypeLabel = new Label("测试类型：");
        testTypeLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #2d3436;");
        ToggleGroup testTypeGroup = new ToggleGroup();
        rbInitialTest = new RadioButton("初测");
        rbRetestTest = new RadioButton("复测");
        rbInitialTest.setToggleGroup(testTypeGroup);
        rbRetestTest.setToggleGroup(testTypeGroup);
        rbInitialTest.setSelected(true);

        // RadioButton样式
        String radioStyle = "-fx-font-size: 11px; -fx-text-fill: #2d3436; -fx-cursor: hand; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;";
        rbInitialTest.setStyle(radioStyle);
        rbRetestTest.setStyle(radioStyle);

        HBox testBox = new HBox(7, rbInitialTest, rbRetestTest);
        testBox.setAlignment(Pos.CENTER_LEFT);
        grid.addRow(row++, testTypeLabel, testBox);

        // 乙方公司名称
        Label contractorLabel = new Label("检测机构：");
        contractorLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #2d3436;");
        grid.add(contractorLabel, 0, row);
        contractorNameField = new TextField();
        contractorNameField.setPromptText("填写检测机构名称，显示在署名、声明等位置");
        contractorNameField.setStyle("-fx-font-size: 11px; -fx-padding: 4px 8px; -fx-border-radius: 4px; -fx-border-color: #dfe6e9; -fx-border-width: 1px; -fx-background-radius: 4px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-background-color: white;");
        grid.add(contractorNameField, 1, row++);

        // 创建通用样式
        String labelStyle = "-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #2d3436;";
        String fieldStyle = "-fx-font-size: 11px; -fx-padding: 4px 8px; -fx-border-radius: 4px; -fx-border-color: #dfe6e9; -fx-border-width: 1px; -fx-background-radius: 4px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-background-color: white;";

        // 渗透测试日期
        Label testDateLabel = new Label("测试时间：");
        testDateLabel.setStyle(labelStyle);
        grid.add(testDateLabel, 0, row);
        testDateField = new TextField();
        testDateField.setPromptText("填写渗透测试时间：于{此处填写}对XX客户进行了渗透测试");
        testDateField.setStyle(fieldStyle);
        grid.add(testDateField, 1, row++);

        // 报告时间
        Label reportDateLabel = new Label("报告日期：");
        reportDateLabel.setStyle(labelStyle);
        grid.add(reportDateLabel, 0, row);
        LocalDate today = LocalDate.now();
        reportYearField = new TextField(String.valueOf(today.getYear()));
        reportYearField.setPromptText("年");
        reportMonthField = new TextField(String.format("%02d", today.getMonthValue()));
        reportMonthField.setPromptText("月");
        reportDayField = new TextField(String.format("%02d", today.getDayOfMonth()));
        reportDayField.setPromptText("日");

        // 为日期字段设置样式
        String dateFieldStyle = "-fx-font-size: 11px; -fx-padding: 4px 6px; -fx-border-radius: 4px; -fx-border-color: #dfe6e9; -fx-border-width: 1px; -fx-background-radius: 4px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-background-color: white; -fx-pref-width: 60px;";
        reportYearField.setStyle(dateFieldStyle);
        reportMonthField.setStyle(dateFieldStyle);
        reportDayField.setStyle(dateFieldStyle);

        HBox reportDateBox = new HBox(4, reportYearField, reportMonthField, reportDayField);
        reportDateBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(reportDateBox, 1, row++);

        // 报告编写人员
        Label reportAuthorLabel = new Label("报告作者：");
        reportAuthorLabel.setStyle(labelStyle);
        grid.add(reportAuthorLabel, 0, row);
        reportAuthorField = new TextField();
        reportAuthorField.setPromptText("填写报告编写人员");
        reportAuthorField.setStyle(fieldStyle);
        grid.add(reportAuthorField, 1, row++);

        // 渗透测试人员
        Label testerLabel = new Label("测试人员：");
        testerLabel.setStyle(labelStyle);
        grid.add(testerLabel, 0, row);
        testerField = new TextField();
        testerField.setPromptText("填写渗透测试人员");
        testerField.setStyle(fieldStyle);
        grid.add(testerField, 1, row++);

        // 项目经理
        Label managerLabel = new Label("项目经理：");
        managerLabel.setStyle(labelStyle);
        grid.add(managerLabel, 0, row);
        managerField = new TextField();
        managerField.setPromptText("填写项目经理");
        managerField.setStyle(fieldStyle);
        grid.add(managerField, 1, row++);

        // 漏洞数量
        Label vulnLabel = new Label("漏洞统计：");
        vulnLabel.setStyle(labelStyle);

        // 漏洞数量字段样式
        String vulnFieldStyle = "-fx-font-size: 11px; -fx-padding: 4px 8px; -fx-border-radius: 4px; -fx-border-color: #dfe6e9; -fx-border-width: 1px; -fx-background-radius: 4px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-background-color: white; -fx-pref-width: 60px; -fx-alignment: center;";

        highVulnField = new TextField();
        highVulnField.setPromptText("高危");
        highVulnField.setStyle(vulnFieldStyle + " -fx-border-color: #ff6b6b;");

        midVulnField = new TextField();
        midVulnField.setPromptText("中危");
        midVulnField.setStyle(vulnFieldStyle + " -fx-border-color: #feca57;");

        lowVulnField = new TextField();
        lowVulnField.setPromptText("低危");
        lowVulnField.setStyle(vulnFieldStyle + " -fx-border-color: #48dbfb;");

        totalVulnField = new TextField();
        totalVulnField.setPromptText("总计");
        totalVulnField.setEditable(false);
        totalVulnField.setStyle(vulnFieldStyle + " -fx-border-color: #1dd1a1; -fx-background-color: #f0fff4; -fx-font-weight: 600;");
        totalVulnField.textProperty().bind(Bindings.createStringBinding(() -> {
            int high = parseIntOrZero(highVulnField.getText());
            int mid = parseIntOrZero(midVulnField.getText());
            int low = parseIntOrZero(lowVulnField.getText());
            return String.valueOf(high + mid + low);
        }, highVulnField.textProperty(), midVulnField.textProperty(), lowVulnField.textProperty()));

        // 添加标签说明
        Label highLabel = new Label("高危");
        highLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #ff6b6b; -fx-font-weight: 600;");
        Label midLabel = new Label("中危");
        midLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #feca57; -fx-font-weight: 600;");
        Label lowLabel = new Label("低危");
        lowLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #48dbfb; -fx-font-weight: 600;");
        Label totalLabel = new Label("总计");
        totalLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #1dd1a1; -fx-font-weight: 600;");

        VBox vulnInputBox = new VBox(2, highVulnField, highLabel);
        VBox vulnInputBox2 = new VBox(2, midVulnField, midLabel);
        VBox vulnInputBox3 = new VBox(2, lowVulnField, lowLabel);
        VBox vulnInputBox4 = new VBox(2, totalVulnField, totalLabel);

        HBox vulnBox = new HBox(6, vulnInputBox, vulnInputBox2, vulnInputBox3, vulnInputBox4);
        vulnBox.setAlignment(Pos.CENTER_LEFT);

        // 创建模板管理组件
        this.templateComboBox = new ComboBox<>();
        templateComboBox.setPrefWidth(150);
        templateComboBox.setPromptText("选择Word报告样式");
        templateComboBox.setStyle("-fx-font-size: 11px; -fx-padding: 4px 8px; -fx-border-radius: 4px; -fx-border-color: #dfe6e9; -fx-border-width: 1px; -fx-background-radius: 4px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-background-color: white;");

        // 删除样式模板按钮
        Button deleteTemplateBtn = new Button("删除样式");
        deleteTemplateBtn.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 4px; -fx-padding: 4px 8px; -fx-font-size: 10px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0;");
        deleteTemplateBtn.setOnAction(e -> deleteSelectedTemplate());

        // 刷新模板列表的方法
        refreshTemplateList(templateComboBox);

        // 为下拉框添加点击刷新功能 - 每次展开下拉框时刷新模板列表
        templateComboBox.setOnShowing(e -> {
            LogUtils.info(MainWindow.class, "模板下拉框即将展开，刷新模板列表");
            refreshTemplateList(templateComboBox);
        });

        // 模板选择事件监听
        templateComboBox.setOnAction(e -> {
            String selectedTemplate = templateComboBox.getSelectionModel().getSelectedItem();
            if (selectedTemplate != null) {
                currentTemplatePath = GlobalConfig.USER_TEMPLATE_DIR + "/" + selectedTemplate;
                LogUtils.info(MainWindow.class, "模板切换到: " + currentTemplatePath);

                // 验证模板目录是否存在
                String fullPath = MiscUtils.getAbsolutePath(currentTemplatePath);
                File templateDir = new File(fullPath);
                LogUtils.info(MainWindow.class, "模板完整路径: " + fullPath);
                LogUtils.info(MainWindow.class, "模板目录存在: " + templateDir.exists());

                if (!templateDir.exists()) {
                    showAlert("警告", "选择的模板目录不存在: " + selectedTemplate + "\n路径: " + fullPath);
                }
            }
        });

        // 创建Word报告样式管理容器 - 使用HBox水平排列，避免VBox导致的垂直拉伸
        Label reportTemplateLabel = new Label("Word报告样式:");
        reportTemplateLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 600; -fx-text-fill: #636e72;");

        HBox templateManageBox = new HBox(8, reportTemplateLabel, templateComboBox, deleteTemplateBtn);
        templateManageBox.setAlignment(Pos.CENTER_RIGHT);

        // 创建一个弹性空间，用于撑开布局
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 创建包含漏洞统计和模板管理的容器，使用HBox而不是BorderPane
        HBox rowContainer = new HBox(15, vulnBox, spacer, templateManageBox);
        rowContainer.setAlignment(Pos.CENTER); // 整体居中对齐

        grid.addRow(row++, vulnLabel, rowContainer);

        // ====================================底部按钮=====================================
        // 按钮样式定义
        String primaryBtnStyle = "-fx-background-color: #4361ee; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 4px; -fx-padding: 6px 12px; -fx-font-size: 11px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(67, 97, 238, 0.2), 3, 0, 0, 1);";
        String primaryBtnHover = "-fx-background-color: #3651de; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 4px; -fx-padding: 6px 12px; -fx-font-size: 11px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(67, 97, 238, 0.4), 4, 0, 0, 1);";

        String successBtnStyle = "-fx-background-color: #26de81; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 4px; -fx-padding: 6px 12px; -fx-font-size: 11px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(38, 222, 129, 0.2), 3, 0, 0, 1);";
        String successBtnHover = "-fx-background-color: #1eb980; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 4px; -fx-padding: 6px 12px; -fx-font-size: 11px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(38, 222, 129, 0.4), 4, 0, 0, 1);";

        String secondaryBtnStyle = "-fx-background-color: #74b9ff; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 4px; -fx-padding: 6px 12px; -fx-font-size: 11px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(116, 185, 255, 0.2), 3, 0, 0, 1);";
        String secondaryBtnHover = "-fx-background-color: #5ba3f5; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 4px; -fx-padding: 6px 12px; -fx-font-size: 11px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(116, 185, 255, 0.4), 4, 0, 0, 1);";

        Button addVulnBtn = new Button("录入漏洞");
        addVulnBtn.setStyle(primaryBtnStyle);
        addVulnBtn.setOnMouseEntered(e -> addVulnBtn.setStyle(primaryBtnHover));
        addVulnBtn.setOnMouseExited(e -> addVulnBtn.setStyle(primaryBtnStyle));

        Button generateReportBtn = new Button("生成报告");
        generateReportBtn.setStyle(successBtnStyle);
        generateReportBtn.setOnMouseEntered(e -> generateReportBtn.setStyle(successBtnHover));
        generateReportBtn.setOnMouseExited(e -> generateReportBtn.setStyle(successBtnStyle));

        Button openVulnEditorBtn = new Button("漏洞库编辑器");
        openVulnEditorBtn.setStyle(secondaryBtnStyle);
        openVulnEditorBtn.setOnMouseEntered(e -> openVulnEditorBtn.setStyle(secondaryBtnHover));
        openVulnEditorBtn.setOnMouseExited(e -> openVulnEditorBtn.setStyle(secondaryBtnStyle));
        // 居中逻辑在最底部
        // ================================================================================

        // 生成报告按钮
        generateReportBtn.setOnAction(e -> {
            // ===================================填写检测逻辑========================================
            // 先清空之前的红框
            resetFieldStyles();

            StringBuilder errorMsg = new StringBuilder();

            // 必填项检测
            if (clientNameField.getText().trim().isEmpty()) {
                markFieldError(clientNameField);
                errorMsg.append("甲方爸爸名称不能为空。\n");
            }
            if (!rbInitialTest.isSelected() && !rbRetestTest.isSelected()) {
                markFieldError(rbInitialTest);
                markFieldError(rbRetestTest);
                errorMsg.append("请选择初测或复测。\n");
            }
            if (contractorNameField.getText().trim().isEmpty()) {
                markFieldError(contractorNameField);
                errorMsg.append("乙方公司名称不能为空。\n");
            }
            if (testDateField.getText().trim().isEmpty()) {
                markFieldError(testDateField);
                errorMsg.append("渗透测试日期不能为空。\n");
            }

            // 报告日期格式检测
            if (!reportYearField.getText().trim().matches("\\d{4}")) {
                markFieldError(reportYearField);
                errorMsg.append("报告年份格式错误（YYYY）。\n");
            }
            if (!reportMonthField.getText().trim().matches("^(0?[1-9]|1[0-2])$")) {
                markFieldError(reportMonthField);
                errorMsg.append("报告月份格式错误（1-12）。\n");
            }
            if (!reportDayField.getText().trim().matches("^(0?[1-9]|[12]\\d|3[01])$")) {
                markFieldError(reportDayField);
                errorMsg.append("报告日期格式错误（1-31）。\n");
            }

            if (reportAuthorField.getText().trim().isEmpty()) {
                markFieldError(reportAuthorField);
                errorMsg.append("报告编写人员不能为空。\n");
            }
            if (testerField.getText().trim().isEmpty()) {
                markFieldError(testerField);
                errorMsg.append("测试人不能为空。\n");
            }
            if (managerField.getText().trim().isEmpty()) {
                markFieldError(managerField);
                errorMsg.append("项目经理不能为空。\n");
            }

            // 如果有错误，弹窗提示并返回
            if (errorMsg.length() > 0) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("输入错误");
                alert.setHeaderText("请修正以下问题后再生成报告：");
                alert.setContentText(errorMsg.toString());
                alert.showAndWait();
                return;
            }

            // ===================================漏洞生成逻辑========================================
            DocObj doc = getDocObjFromUI();
            System.out.println("生成报告，内容示例：");
            System.out.println("甲方爸爸名称：" + doc.getCustomerName());
            System.out.println("是否初测：" + doc.getIsFirsrTest());
            System.out.println("乙方公司名称：" + doc.getSignatureName());
            System.out.println("报告日期：" + doc.getReportTime());
            System.out.println("报告编写人员：" + doc.getReporter());
            System.out.println("测试日期：" + doc.getTestTime());
            System.out.println("测试人：" + doc.getTesterName());
            System.out.println("项目经理：" + doc.getPmName());
            System.out.println("漏洞数量，高：" + doc.getVulHighCount() +
                    " 中：" + doc.getVulMediumCount() +
                    " 低：" + doc.getVulLowCount() +
                    " 总：" + doc.getVulAllCount());

            // ===================================生成报告原始内容=======================================
            DocObj docObj = getDocObjFromUI();
            String docContent = DocUtils.contentGen(docObj, getCurrentTemplatePath());

            // ===================================生成漏洞相关========================================
            JSONArray unitsArray = JSON.parseArray(FileUtils.readFile(MiscUtils.getAbsolutePath(VULN_TREE_PATH)));
            ReportData reportData = new ReportData();

            // 遍历单位
            for (int i = 0; i < unitsArray.size(); i++) {
                JSONObject unitObj = unitsArray.getJSONObject(i);
                String unitName = unitObj.getString("unit");

                // 查找或创建 Unit
                Unit unit = null;
                for (Unit u : reportData.getUnits()) {
                    if (u.getUnitName().equals(unitName)) {
                        unit = u;
                        break;
                    }
                }
                if (unit == null) {
                    unit = new Unit(unitName);
                    reportData.getUnits().add(unit);
                }
                // 遍历系统
                JSONArray systemsArray = unitObj.getJSONArray("systems");
                if (systemsArray != null) {
                    for (int j = 0; j < systemsArray.size(); j++) {
                        JSONObject systemObj = systemsArray.getJSONObject(j);
                        String systemName = systemObj.getString("system");

                        // 查找或创建 SystemInfo
                        SystemInfo system = null;
                        for (SystemInfo s : unit.getSystems()) {
                            if (s.getSystemName().equals(systemName)) {
                                system = s;
                                break;
                            }
                        }
                        if (system == null) {
                            system = new SystemInfo(systemName);
                            unit.getSystems().add(system);
                        }
                        // 遍历漏洞
                        JSONArray vulnsArray = systemObj.getJSONArray("vulns");
                        if (vulnsArray != null) {
                            for (int k = 0; k < vulnsArray.size(); k++) {
                                JSONObject vulnObj = vulnsArray.getJSONObject(k);
                                String vulnName = vulnObj.getString("name");

                                // 处理空漏洞描述，VulnerabilityService类有自动获取的逻辑
                                try {
                                    Vulnerability vuln = new Vulnerability(vulnName);

                                    String desc = vulnObj.getString("desc");
                                    if (desc != null && !desc.trim().isEmpty()) {
                                        vuln.setVulDesc(desc);
                                    }

                                    String fix = vulnObj.getString("fix");
                                    if (fix != null && !fix.trim().isEmpty()) {
                                        vuln.setVulFixSuggestion(fix);
                                    }

                                    String harm = vulnObj.getString("harm");
                                    if (harm != null && !harm.trim().isEmpty()) {
                                        vuln.setVulHazards(harm);
                                    }

                                    String level = vulnObj.getString("level");
                                    if (level != null && !level.trim().isEmpty()) {
                                        vuln.setRiskLevel(level);
                                    }

                                    String repaired = vulnObj.getString("repaired");
                                    if (repaired != null && !repaired.trim().isEmpty()) {
                                        vuln.setIsFixed(repaired);
                                    }

                                    system.getVulnerabilities().add(vuln);
                                } catch (FileNotFoundException ex) {
                                    LogUtils.error(MainWindow.class, "加载漏洞库失败" + ex.getMessage());
                                }
                            }
                        }
                    }
                }
            }

            // ===========================最终生成漏洞内容============================================
            String mainContent = DocUtils.mainContentGen(reportData, docObj);
            String finalContent = docContent.replace("{{{{{MainContent}}}}}", mainContent);
            try {
                String reportFilePath = DocUtils.docGen(getCurrentTemplatePath(), finalContent, docObj);
                // 弹窗提示生成成功并显示路径，带打开按钮
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("生成成功");
                successAlert.setHeaderText("报告生成成功");
                successAlert.setContentText("报告路径：" + reportFilePath);

                ButtonType openFolderBtn = new ButtonType("打开所在文件夹");
                successAlert.getButtonTypes().add(openFolderBtn);
                successAlert.getButtonTypes().add(ButtonType.CLOSE);
                // 显示弹窗并等待用户选择
                successAlert.showAndWait().ifPresent(type -> {
                    if (type == openFolderBtn) {
                        openFolderAndSelectFileWithFallback(reportFilePath);
                    }
                });
            } catch (IOException ex) {
                LogUtils.error(MainWindow.class, "生成报告失败" + ex.getMessage());
            }
        });

        // =================================== 自适应 + 滚动封装 ====================================

        mainVBox = new VBox(12);  // 间距12
        mainVBox.setPadding(new Insets(12));
        mainVBox.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 6px;");
        // 先初始化模板UI控件
        initTemplateUI();
        // 再把 grid 加进 mainVBox
        mainVBox.getChildren().add(grid);

        // =====================================底部按钮容器==================================
        // 创建报告模板制作按钮
        String templateBtnStyle = "-fx-background-color: #ff9ff3; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 4px; -fx-padding: 6px 12px; -fx-font-size: 11px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(255, 159, 243, 0.2), 3, 0, 0, 1);";
        String templateBtnHover = "-fx-background-color: #ff6bb6; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 4px; -fx-padding: 6px 12px; -fx-font-size: 11px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(255, 159, 243, 0.4), 4, 0, 0, 1);";

        reportTemplateBtn = new Button("报告模板制作");
        reportTemplateBtn.setStyle(templateBtnStyle);
        reportTemplateBtn.setOnMouseEntered(e -> reportTemplateBtn.setStyle(templateBtnHover));
        reportTemplateBtn.setOnMouseExited(e -> reportTemplateBtn.setStyle(templateBtnStyle));

        // 报告模板制作按钮点击事件
        reportTemplateBtn.setOnAction(e -> openReportTemplateMaker());

        // 设置其他按钮的事件处理
        openVulnEditorBtn.setOnAction(e -> {
            vuLnEditorWindow = new VulnEditorWindow();
            vuLnEditorWindow.show();
        });

        addVulnBtn.setOnAction(e -> {
            VulnTreeInputWindow vulnTreeInputWindow = new VulnTreeInputWindow();
            vulnTreeInputWindow.show();
        });

        // 右侧其他按钮容器
        HBox rightButtonsHBox = new HBox(10, openVulnEditorBtn, addVulnBtn, generateReportBtn);
        rightButtonsHBox.setAlignment(Pos.CENTER_RIGHT);
        rightButtonsHBox.setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        // 创建底部总容器，使用BorderPane布局
        BorderPane bottomPane = new BorderPane();
        bottomPane.setLeft(reportTemplateBtn);
        bottomPane.setRight(rightButtonsHBox);
        bottomPane.setPadding(new Insets(10, 15, 6, 15));
        bottomPane.setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        // 将底部容器添加到主VBox
        mainVBox.getChildren().add(bottomPane);
        // ================================================================================

        scrollPane = new ScrollPane(mainVBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPadding(new Insets(6));
        scrollPane.setStyle("-fx-background-color: #f5f7fa; -fx-border-color: transparent; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
    }

    /**
     * 解析字符串为整数，失败返回0
     * @param text 要解析的字符串
     * @return 解析后的整数，失败返回0
     */
    private int parseIntOrZero(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception e) {
//            LogUtils.error(MainWindow.class, "解析整数失败" + text);
            return 0;
        }
    }

    /**
     * 获取主视图
     * @return 主视图的父节点
     */
    public Parent getView() {
        return scrollPane;
    }

    /**
     * 从UI获取数据并封装成DocObj
     * @return 封装好的DocObj对象
     */
    public DocObj getDocObjFromUI() {
        DocObj doc = new DocObj();

        doc.setCustomerName(clientNameField.getText().trim());

        // 初测还是复测
        if (rbInitialTest.isSelected()) {
            doc.setIsFirsrTest("初测");
        } else if (rbRetestTest.isSelected()) {
            doc.setIsFirsrTest("复测");
        }

        doc.setSignatureName(contractorNameField.getText().trim());

        // 组合报告时间，格式 yyyy年M月d日
        String reportDateStr = reportYearField.getText().trim() + "年" +
                reportMonthField.getText().trim() + "月" +
                reportDayField.getText().trim() + "日";
        doc.setReportTime(reportDateStr);

        doc.setReporter(reportAuthorField.getText().trim());
        doc.setTestTime(testDateField.getText().trim());
        doc.setTesterName(testerField.getText().trim());
        doc.setPmName(managerField.getText().trim());

        // 漏洞数量，转换为int
        doc.setVulHighCount(parseIntOrZero(highVulnField.getText()));
        doc.setVulMediumCount(parseIntOrZero(midVulnField.getText()));
        doc.setVulLowCount(parseIntOrZero(lowVulnField.getText()));
        doc.setVulAllCount(parseIntOrZero(totalVulnField.getText()));

        return doc;
    }

    /**
     * 标记字段为错误状态
     * @param field 要标记的字段
     */
    private void markFieldError(Control field) {
        field.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
    }

    /**
     * 重置所有字段的样式
     */
    private void resetFieldStyles() {
        Control[] allFields = {
                clientNameField, contractorNameField, testDateField,
                reportYearField, reportMonthField, reportDayField,
                reportAuthorField, testerField, managerField,
                rbInitialTest, rbRetestTest
        };
        for (Control field : allFields) {
            field.setStyle(""); // 清空样式
        }
    }

    /**
     * 初始化模板UI
     */
    private void initTemplateUI() {
        // 客户配置区域标题
        Label templateTitle = new Label("客户配置管理");
        templateTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #2d3436;");

        HBox templateBox = new HBox(8);
        templateBox.setAlignment(Pos.CENTER);
        templateBox.setPadding(new Insets(6, 0, 5, 0));
        templateBox.setStyle("-fx-background-color: #f8f9fa; -fx-border-radius: 5px; -fx-background-radius: 5px; -fx-padding: 8px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        Label templateLabel = new Label("选择客户配置：");
        templateLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 600; -fx-text-fill: #636e72;");

        customerTemplateComboBox = new ComboBox<>();
        customerTemplateComboBox.setPromptText("选择客户配置");
        customerTemplateComboBox.setStyle("-fx-font-size: 11px; -fx-padding: 4px 6px; -fx-border-radius: 4px; -fx-border-color: #dfe6e9; -fx-border-width: 1px; -fx-background-radius: 4px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-background-color: white;");

        Label nameLabel = new Label("配置名称：");
        nameLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 600; -fx-text-fill: #636e72;");

        templateNameField = new TextField();
        templateNameField.setPromptText("输入配置名称");
        templateNameField.setStyle("-fx-font-size: 11px; -fx-padding: 4px 6px; -fx-border-radius: 4px; -fx-border-color: #dfe6e9; -fx-border-width: 1px; -fx-background-radius: 4px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-background-color: white;");

        // 模板按钮样式
        String saveBtnStyle = "-fx-background-color: #26de81; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 3px; -fx-padding: 4px 8px; -fx-font-size: 9px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(38, 222, 129, 0.2), 2, 0, 0, 1);";
        String saveBtnHover = "-fx-background-color: #1eb980; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 3px; -fx-padding: 4px 8px; -fx-font-size: 9px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(38, 222, 129, 0.4), 3, 0, 0, 1);";

        String deleteBtnStyle = "-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 3px; -fx-padding: 4px 8px; -fx-font-size: 9px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(255, 107, 107, 0.2), 2, 0, 0, 1);";
        String deleteBtnHover = "-fx-background-color: #ee5a52; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 3px; -fx-padding: 4px 8px; -fx-font-size: 9px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(255, 107, 107, 0.4), 3, 0, 0, 1);";

        saveTemplateButton = new Button("保存配置");
        saveTemplateButton.setStyle(saveBtnStyle);
        saveTemplateButton.setOnMouseEntered(e -> saveTemplateButton.setStyle(saveBtnHover));
        saveTemplateButton.setOnMouseExited(e -> saveTemplateButton.setStyle(saveBtnStyle));

        deleteTemplateButton = new Button("删除配置");
        deleteTemplateButton.setStyle(deleteBtnStyle);
        deleteTemplateButton.setDisable(true); // 默认禁用，没选中配置时不可用
        deleteTemplateButton.setOnMouseEntered(e -> {
            if (!deleteTemplateButton.isDisabled()) {
                deleteTemplateButton.setStyle(deleteBtnHover);
            }
        });
        deleteTemplateButton.setOnMouseExited(e -> {
            if (!deleteTemplateButton.isDisabled()) {
                deleteTemplateButton.setStyle(deleteBtnStyle);
            }
        });

        // 创建选择区域和保存区域
        HBox selectBox = new HBox(8, templateLabel, customerTemplateComboBox);
        HBox nameBox = new HBox(8, nameLabel, templateNameField);
        HBox buttonBox = new HBox(8, saveTemplateButton, deleteTemplateButton);

        selectBox.setAlignment(Pos.CENTER_LEFT);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        templateBox.getChildren().addAll(selectBox, nameBox, buttonBox);

        // 添加说明文本
        Label templateDescLabel = new Label("保存和管理客户信息配置，包含客户名称、项目经理等基本信息");
        templateDescLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #95a5a6; -fx-padding: 0 0 4px 0;");

        // 将客户配置标题和容器添加到主界面
        VBox templateContainer = new VBox(6, templateTitle, templateDescLabel, templateBox);
        mainVBox.getChildren().add(0, templateContainer);

        // 加载模板列表
        loadTemplateList();

        // 下拉框选择模板 → 加载数据
        customerTemplateComboBox.setOnAction(e -> {
            String selected = customerTemplateComboBox.getValue();
            if (selected != null) {
                loadTemplate(selected);
                deleteTemplateButton.setDisable(false);
            } else {
                deleteTemplateButton.setDisable(true);
            }
        });

        // 保存按钮
        saveTemplateButton.setOnAction(e -> {
            String name = templateNameField.getText().trim();
            if (name.isEmpty()) {
                showAlert("错误", "模板名不能为空！");
                return;
            }
            saveTemplate(name);
            loadTemplateList(); // 刷新模板列表
        });

        // 删除按钮
        deleteTemplateButton.setOnAction(e -> {
            String selected = customerTemplateComboBox.getValue();
            if (selected == null) {
                showAlert("错误", "请选择一个模板删除！");
                return;
            }
            // 确认删除弹窗
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("确认删除");
            confirm.setHeaderText(null);
            confirm.setContentText("确认删除模板：" + selected + " ?");
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    File file = new File(MiscUtils.getAbsolutePath(COMPANY_TEMPLATE_DIR), selected + ".json");
                    if (file.exists() && file.delete()) {
                        showAlert("成功", "模板已删除！");
                        loadTemplateList();
                        customerTemplateComboBox.setValue(null);
                        deleteTemplateButton.setDisable(true);
                        // 清空界面数据，避免残留
                        templateNameField.clear();
                        setCurrentData(new JSONObject());
                    } else {
                        showAlert("错误", "删除模板失败！");
                    }
                }
            });
        });

        // 模板UI已在上面添加到主界面
    }

    /**
     * 加载模板列表
     */
    private void loadTemplateList() {
        File dir = new File(MiscUtils.getAbsolutePath(COMPANY_TEMPLATE_DIR));
        if (!dir.exists()) {
            dir.mkdirs();
        }
        customerTemplateComboBox.getItems().clear();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File f : files) {
                customerTemplateComboBox.getItems().add(f.getName().replace(".json", ""));
            }
        }
    }

    /**
     * 保存模板
     * @param name 模板名
     */
    private void saveTemplate(String name) {
        try {
            // 获取当前界面数据对象
            JSONObject currentData = getCurrentData();
            String json = JSON.toJSONString(currentData, JSONWriter.Feature.PrettyFormat);
            System.out.println(json);
            Files.write(Paths.get(MiscUtils.getAbsolutePath(COMPANY_TEMPLATE_DIR), name + ".json"), json.getBytes(StandardCharsets.UTF_8));
            showAlert("成功", "模板已保存！");
        } catch (IOException ex) {
            LogUtils.error(MainWindow.class, "保存模板失败" + ex.getMessage());
            showAlert("错误", "保存模板失败: " + ex.getMessage());
        }
    }

    /**
     * 加载模板
     * @param name 模板名
     */
    private void loadTemplate(String name) {
        try {
            String json = new String(Files.readAllBytes(Paths.get(MiscUtils.getAbsolutePath(COMPANY_TEMPLATE_DIR), name + ".json")), StandardCharsets.UTF_8);
            Object data = JSON.parseObject(json, Object.class);
            setCurrentData(data); // 将数据填充到界面
        } catch (IOException ex) {
            LogUtils.error(MainWindow.class, "加载模板失败" + ex.getMessage());
        }
    }

    /**
     * 弹窗提示
     * @param title 弹窗标题
     * @param msg 弹窗内容
     */
    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    /**
     * 获取当前界面数据
     * @return 数据对象
     */
    private JSONObject getCurrentData() {
        JSONObject json = new JSONObject();

        json.put("clientName", clientNameField.getText().trim());
        json.put("isFirstTest", rbInitialTest.isSelected() ? "初测" : (rbRetestTest.isSelected() ? "复测" : ""));
        json.put("contractorName", contractorNameField.getText().trim());
        json.put("testDate", testDateField.getText().trim());
        json.put("reportYear", reportYearField.getText().trim());
        json.put("reportMonth", reportMonthField.getText().trim());
        json.put("reportDay", reportDayField.getText().trim());
        json.put("reportAuthor", reportAuthorField.getText().trim());
        json.put("tester", testerField.getText().trim());
        json.put("manager", managerField.getText().trim());
        json.put("highVuln", highVulnField.getText().trim());
        json.put("midVuln", midVulnField.getText().trim());
        json.put("lowVuln", lowVulnField.getText().trim());
        // totalVulnField是自动绑定的，不需要保存

        return json;
    }

    /**
     * 填充数据到界面
     * @param data 数据对象
     */
    private void setCurrentData(Object data) {
        if (!(data instanceof JSONObject)) {
            return;
        }
        JSONObject json = (JSONObject) data;

        clientNameField.setText(json.getString("clientName"));

        String isFirstTest = json.getString("isFirstTest");
        if ("初测".equals(isFirstTest)) {
            rbInitialTest.setSelected(true);
        } else if ("复测".equals(isFirstTest)) {
            rbRetestTest.setSelected(true);
        } else {
            rbInitialTest.setSelected(false);
            rbRetestTest.setSelected(false);
        }

        contractorNameField.setText(json.getString("contractorName"));
        testDateField.setText(json.getString("testDate"));
        reportYearField.setText(json.getString("reportYear"));
        reportMonthField.setText(json.getString("reportMonth"));
        reportDayField.setText(json.getString("reportDay"));
        reportAuthorField.setText(json.getString("reportAuthor"));
        testerField.setText(json.getString("tester"));
        managerField.setText(json.getString("manager"));
        highVulnField.setText(json.getString("highVuln"));
        midVulnField.setText(json.getString("midVuln"));
        lowVulnField.setText(json.getString("lowVuln"));
    }

    /**
     * 打开文件所在文件夹并选中文件
     * @param filePath 文件路径
     */
    private void openFolderAndSelectFileWithFallback(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            showAlert("错误", "报告文件不存在！");
            return;
        }
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("win")) {
                // Windows
                String cmd = "explorer /select,\"" + file.getAbsolutePath() + "\"";
                Process p = Runtime.getRuntime().exec(cmd);
                int exitCode = p.waitFor();
                if (exitCode != 0) {
                    // 打开失败，退回打开文件夹
                    Desktop.getDesktop().open(file.getParentFile());
                }
            } else if (os.contains("mac")) {
                // macOS
                Process p = Runtime.getRuntime().exec(new String[]{"open", "-R", file.getAbsolutePath()});
                int exitCode = p.waitFor();
                if (exitCode != 0) {
                    Desktop.getDesktop().open(file.getParentFile());
                }
            } else {
                // Linux 或其他系统，直接打开文件夹
                Desktop.getDesktop().open(file.getParentFile());
            }
        } catch (Exception e) {
            // 任意异常，退回打开文件夹
            LogUtils.error(MainWindow.class, "打开文件所在文件夹失败" + e.getMessage());
            try {
                Desktop.getDesktop().open(file.getParentFile());
            } catch (IOException ex) {
                LogUtils.error(MainWindow.class, "打开文件夹失败" + ex.getMessage());
                showAlert("错误", "无法打开文件夹：" + ex.getMessage());
            }
        }
    }

    /**
     * 打开报告模板制作窗口
     */
    private void openReportTemplateMaker() {
        ReportTemplateMakerWindow templateMaker = new ReportTemplateMakerWindow();
        templateMaker.show();

        // 模板制作窗口关闭后刷新模板列表
        templateMaker.getView().sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((obs2, oldWindow, newWindow) -> {
                    if (oldWindow != null) {
                        // 窗口关闭后刷新模板列表
                        Platform.runLater(() -> {
                            // 查找模板下拉框并刷新
                            if (templateComboBox != null) {
                                refreshTemplateList(templateComboBox);
                            }
                        });
                    }
                });
            }
        });
    }

    /**
     * 刷新模板列表
     */
    private void refreshTemplateList(ComboBox<String> comboBox) {
        comboBox.getItems().clear();

        String templateDir = GlobalConfig.USER_TEMPLATE_DIR;
        File dir = new File(MiscUtils.getAbsolutePath(templateDir));

        if (dir.exists() && dir.isDirectory()) {
            File[] templateDirs = dir.listFiles(File::isDirectory);

            if (templateDirs != null) {
                for (File templateDirItem : templateDirs) {
                    // 跳过隐藏目录和系统目录
                    if (templateDirItem.getName().startsWith(".")) {
                        continue;
                    }

                    // 检查是否包含模板元文件（word目录）
                    File wordDir = new File(templateDirItem, "word");
                    if (wordDir.exists() && wordDir.isDirectory()) {
                        comboBox.getItems().add(templateDirItem.getName());

                        // 如果是当前选中的模板，设置选中状态
                        String expectedPath = GlobalConfig.USER_TEMPLATE_DIR + "/" + templateDirItem.getName();
                        if (expectedPath.equals(currentTemplatePath)) {
                            comboBox.getSelectionModel().select(templateDirItem.getName());
                        }
                    }
                }
            }
        }

        LogUtils.info(MainWindow.class, "已加载 " + comboBox.getItems().size() + " 个模板");
    }

    /**
     * 删除选中的模板
     */
    private void deleteSelectedTemplate() {
        if (templateComboBox == null) {
            showAlert("提示", "模板组件未初始化");
            return;
        }

        String selectedTemplate = templateComboBox.getSelectionModel().getSelectedItem();
        if (selectedTemplate == null || selectedTemplate.trim().isEmpty()) {
            showAlert("提示", "请先选择要删除的模板");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("确认删除");
        confirmAlert.setHeaderText("删除模板: " + selectedTemplate);
        confirmAlert.setContentText("确定要删除模板 '" + selectedTemplate + "' 吗？\n此操作不可撤销，模板相关的所有文件都将被删除。");

        ButtonType confirmButton = new ButtonType("确认删除", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmAlert.getButtonTypes().setAll(confirmButton, cancelButton);

        Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == confirmButton) {
            try {
                String templatePath = GlobalConfig.USER_TEMPLATE_DIR + "/" + selectedTemplate;
                File templateDir = new File(MiscUtils.getAbsolutePath(templatePath));

                if (templateDir.exists() && templateDir.isDirectory()) {
                    // 递归删除模板目录
                    deleteDirectory(templateDir);

                    // 刷新模板列表
                    refreshTemplateList(templateComboBox);

                    // 如果删除的是当前使用的模板，重置模板路径
                    if (currentTemplatePath != null && currentTemplatePath.contains(selectedTemplate)) {
                        currentTemplatePath = null;
                    }

                    showAlert("成功", "模板 '" + selectedTemplate + "' 已成功删除");
                    LogUtils.info(MainWindow.class, "模板删除成功: " + selectedTemplate);
                } else {
                    showAlert("错误", "模板目录不存在或已被删除");
                }

            } catch (Exception e) {
                LogUtils.error(MainWindow.class, "删除模板失败: " + selectedTemplate, e);
                showAlert("错误", "删除模板失败：" + e.getMessage());
            }
        }
    }

    /**
     * 递归删除目录及其所有内容
     */
    private void deleteDirectory(File directory) throws IOException {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        if (!directory.delete()) {
            throw new IOException("无法删除文件或目录: " + directory.getAbsolutePath());
        }
    }

    /**
     * 获取当前使用的模板路径
     */
    public String getCurrentTemplatePath() {
        if (currentTemplatePath == null || currentTemplatePath.trim().isEmpty()) {
            // 默认返回硬编码的模板路径
            return GlobalConfig.DOC_TEMPLATE_PATH;
        }
        return currentTemplatePath;
    }

}
