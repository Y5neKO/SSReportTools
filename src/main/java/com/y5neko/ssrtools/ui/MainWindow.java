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
import javafx.beans.binding.Bindings;
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
    private ComboBox<String> templateComboBox;
    private TextField templateNameField;
    private Button saveTemplateButton;
    private VBox mainVBox;
    private Button deleteTemplateButton;

    // 漏洞库编辑按钮
    private VulnEditorWindow vuLnEditorWindow;

    /**
     * 构造函数
     */
    public MainWindow() {
        grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(15);
        grid.setVgap(10);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.NEVER);
        col1.setMinWidth(Region.USE_PREF_SIZE);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        int row = 0;

        // 甲方爸爸名称
        grid.add(new Label("甲方爸爸名称："), 0, row);
        clientNameField = new TextField();
        clientNameField.setPromptText("填写甲方公司名称，显示在报告顶部、声明及文件名等位置");
        grid.add(clientNameField, 1, row++);

        // 是否初测
        Label testTypeLabel = new Label("是否初测：");
        ToggleGroup testTypeGroup = new ToggleGroup();
        rbInitialTest = new RadioButton("初测");
        rbRetestTest = new RadioButton("复测");
        rbInitialTest.setToggleGroup(testTypeGroup);
        rbRetestTest.setToggleGroup(testTypeGroup);
        rbInitialTest.setSelected(true);
        HBox testBox = new HBox(10, rbInitialTest, rbRetestTest);
        grid.addRow(row++, testTypeLabel, testBox);

        // 乙方公司名称
        grid.add(new Label("乙方公司名称："), 0, row);
        contractorNameField = new TextField();
        contractorNameField.setPromptText("填写乙方公司名称，显示在署名、声明等位置");
        grid.add(contractorNameField, 1, row++);

        // 渗透测试日期
        grid.add(new Label("渗透测试日期："), 0, row);
        testDateField = new TextField();
        testDateField.setPromptText("填写渗透测试日期：于{此处填写}对XX客户进行了渗透测试");
        grid.add(testDateField, 1, row++);

        // 报告时间
        grid.add(new Label("报告编写日期："), 0, row);
        LocalDate today = LocalDate.now();
        reportYearField = new TextField(String.valueOf(today.getYear()));
        reportYearField.setPromptText("年");
        reportMonthField = new TextField(String.format("%02d", today.getMonthValue()));
        reportMonthField.setPromptText("月");
        reportDayField = new TextField(String.format("%02d", today.getDayOfMonth()));
        reportDayField.setPromptText("日");
        HBox reportDateBox = new HBox(5, reportYearField, reportMonthField, reportDayField);
        grid.add(reportDateBox, 1, row++);

        // 报告编写人员
        grid.add(new Label("报告编写人员："), 0, row);
        reportAuthorField = new TextField();
        reportAuthorField.setPromptText("填写报告编写人员");
        grid.add(reportAuthorField, 1, row++);

        // 渗透测试人员
        grid.add(new Label("渗透测试人员："), 0, row);
        testerField = new TextField();
        testerField.setPromptText("填写渗透测试人员");
        grid.add(testerField, 1, row++);

        // 项目经理
        grid.add(new Label("项目经理："), 0, row);
        managerField = new TextField();
        managerField.setPromptText("填写项目经理");
        grid.add(managerField, 1, row++);

        // 漏洞数量
        Label vulnLabel = new Label("漏洞数量：");
        highVulnField = new TextField();
        highVulnField.setPromptText("高");
        midVulnField = new TextField();
        midVulnField.setPromptText("中");
        lowVulnField = new TextField();
        lowVulnField.setPromptText("低");
        totalVulnField = new TextField();
        totalVulnField.setPromptText("总数");
        totalVulnField.setEditable(false);
        totalVulnField.textProperty().bind(Bindings.createStringBinding(() -> {
            int high = parseIntOrZero(highVulnField.getText());
            int mid = parseIntOrZero(midVulnField.getText());
            int low = parseIntOrZero(lowVulnField.getText());
            return String.valueOf(high + mid + low);
        }, highVulnField.textProperty(), midVulnField.textProperty(), lowVulnField.textProperty()));
        HBox vulnBox = new HBox(10, highVulnField, midVulnField, lowVulnField, totalVulnField);
        grid.addRow(row++, vulnLabel, vulnBox);

        // ====================================底部按钮=====================================
        Button addVulnBtn = new Button("录入漏洞");
        Button generateReportBtn = new Button("生成报告");
        Button openVulnEditorBtn = new Button("漏洞库编辑器");
        // 居中逻辑在最底部
        // ================================================================================

        // ============================ 底部按钮事件 =============================
        // 漏洞库编辑器按钮
        openVulnEditorBtn.setOnAction(e -> {
            vuLnEditorWindow = new VulnEditorWindow();
            vuLnEditorWindow.show();
        });
        // 录入漏洞按钮
        addVulnBtn.setOnAction(e -> {
            VulnTreeInputWindow vulnTreeInputWindow = new VulnTreeInputWindow();
            vulnTreeInputWindow.show();
        });
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
            String docContent = DocUtils.contentGen(docObj);

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
            String finalContent = docContent.replaceAll("\\{\\{\\{\\{\\{MainContent}}}}}", mainContent);
            try {
                String reportFilePath = DocUtils.docGen(null, finalContent, docObj);
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

        mainVBox = new VBox(10);  // 间距10
        mainVBox.setPadding(new Insets(20));
        // 先初始化模板UI控件
        initTemplateUI();
        // 再把 grid 加进 mainVBox
        mainVBox.getChildren().add(grid);

        // =====================================底部按钮容器（用于居中）==================================
        HBox buttonsHBox = new HBox(20, openVulnEditorBtn, addVulnBtn, generateReportBtn);
        buttonsHBox.setAlignment(Pos.CENTER);
        buttonsHBox.setMaxWidth(Double.MAX_VALUE);
        // 将按钮容器放到底部，整个宽度撑满，按钮居中
        mainVBox.getChildren().add(buttonsHBox);
        // ================================================================================

        scrollPane = new ScrollPane(mainVBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPadding(new Insets(10));
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
        HBox templateBox = new HBox(10);
        templateBox.setAlignment(Pos.CENTER);

        templateComboBox = new ComboBox<>();
        templateComboBox.setPromptText("选择甲方模板");

        templateNameField = new TextField();
        templateNameField.setPromptText("模板名");

        saveTemplateButton = new Button("保存到模板");

        deleteTemplateButton = new Button("删除模板");
        deleteTemplateButton.setDisable(true); // 默认禁用，没选中模板时不可用

        templateBox.getChildren().addAll(new Label("模板:"), templateComboBox,
                deleteTemplateButton,
                new Label("模板名:"), templateNameField, saveTemplateButton);

        // 加载模板列表
        loadTemplateList();

        // 下拉框选择模板 → 加载数据
        templateComboBox.setOnAction(e -> {
            String selected = templateComboBox.getValue();
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
            String selected = templateComboBox.getValue();
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
                        templateComboBox.setValue(null);
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

        // 将 templateBox 加到主界面最上方
        mainVBox.getChildren().add(0, templateBox);
    }

    /**
     * 加载模板列表
     */
    private void loadTemplateList() {
        File dir = new File(MiscUtils.getAbsolutePath(COMPANY_TEMPLATE_DIR));
        if (!dir.exists()) {
            dir.mkdirs();
        }
        templateComboBox.getItems().clear();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File f : files) {
                templateComboBox.getItems().add(f.getName().replace(".json", ""));
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

}
