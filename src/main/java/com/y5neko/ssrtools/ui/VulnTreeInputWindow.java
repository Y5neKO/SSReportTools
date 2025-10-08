package com.y5neko.ssrtools.ui;

import com.alibaba.fastjson2.JSON;
import com.y5neko.ssrtools.models.vulntree.SystemEntry;
import com.y5neko.ssrtools.models.vulntree.UnitEntry;
import com.y5neko.ssrtools.models.vulntree.Vuln;
import com.y5neko.ssrtools.services.VulnerabilityService;
import com.y5neko.ssrtools.utils.LogUtils;
import com.y5neko.ssrtools.utils.MiscUtils;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.function.Consumer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.y5neko.ssrtools.config.GlobalConfig.VULN_TREE_PATH;

/**
 * 漏洞录入窗口
 */
public class VulnTreeInputWindow {

    private final List<UnitEntry> unitEntries = new ArrayList<>();
    private final File saveFile = new File(MiscUtils.getAbsolutePath(VULN_TREE_PATH));
    private final VBox root = new VBox(10);
    private VulnerabilityService vulnerabilityService;

    /**
     * 显示漏洞录入窗口
     */
    public void show() {
        // 初始化漏洞服务
        try {
            vulnerabilityService = new VulnerabilityService();
        } catch (FileNotFoundException e) {
            LogUtils.error(VulnTreeInputWindow.class, "漏洞库文件未找到：" + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "初始化错误", "漏洞库文件未找到，请检查配置");
            return;
        }

        Stage stage = new Stage();
        stage.setTitle("漏洞录入");

        // 美化整体样式
        root.setStyle("-fx-background-color: #f5f6fa;");
        root.setPadding(new Insets(20));

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-border-color: #e9ecef;");

        VBox container = new VBox(15);
        container.setPadding(new Insets(10));
        scrollPane.setContent(container);

        // 美化按钮样式
        String primaryBtnStyle = "-fx-background-color: #4361ee; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-radius: 6px; -fx-padding: 8px 16px;";
        String dangerBtnStyle = "-fx-background-color: #f53e57; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-radius: 6px; -fx-padding: 8px 16px;";
        String successBtnStyle = "-fx-background-color: #26de81; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-radius: 6px; -fx-padding: 8px 16px;";

        Button addUnitBtn = new Button("+ 添加单位");
        addUnitBtn.setStyle(primaryBtnStyle);
        addUnitBtn.setOnAction(e -> {
            VBox unitBox = createUnitBlock(container);
            container.getChildren().add(unitBox);
        });

        Button clearBtn = new Button("清空");
        clearBtn.setStyle(dangerBtnStyle);
        clearBtn.setOnAction(e -> {
            container.getChildren().clear();
            unitEntries.clear();
        });

        Button saveBtn = new Button("保存");
        saveBtn.setStyle(successBtnStyle);
        saveBtn.setOnAction(e -> {
            if (collectDataFromUI(container)) {
                // 调试：检查修复建议字段的换行符
                if (!unitEntries.isEmpty() && !unitEntries.get(0).systems.isEmpty() && !unitEntries.get(0).systems.get(0).vulns.isEmpty()) {
                    String fixText = unitEntries.get(0).systems.get(0).vulns.get(0).fix;
                    int lineCount = fixText.split("\n").length;
                    LogUtils.info(VulnTreeInputWindow.class, "保存的修复建议内容（行数：" + lineCount + "）：" + fixText.replace("\n", "[换行]"));
                }

                saveData();
                showAlert(Alert.AlertType.INFORMATION, "保存成功", "数据已成功保存");
            }
        });

        // 按钮容器样式
        HBox btnBox = new HBox(12, addUnitBtn, clearBtn, saveBtn);
        btnBox.setAlignment(Pos.CENTER);
        btnBox.setPadding(new Insets(0, 0, 20, 0));

        root.getChildren().addAll(btnBox, scrollPane);

        loadDataToUI(container);

        Scene scene = new Scene(root, 1200, 650);
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
    }

    /**
     * 创建单位块
     * @param container 容器
     * @return 单位块
     */
    private VBox createUnitBlock(VBox container) {
        VBox unitBox = new VBox(10);
        unitBox.setStyle("-fx-border-color: #dfe6e9; -fx-border-radius: 8px; -fx-background-color: white; -fx-padding: 15px; -fx-background-radius: 8px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 5, 0, 0, 2);");

        HBox titleBox = new HBox(12);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Label unitLabel = new Label("【单位】");
        unitLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2d3436;");

        TextField unitName = new TextField();
        unitName.setPromptText("单位名称（必填）");
        unitName.setStyle("-fx-font-size: 14px; -fx-background-color: #f8f9fa; -fx-border-color: #e9ecef; -fx-border-radius: 4px; -fx-padding: 8px;");
        unitName.setTooltip(new Tooltip("请输入单位名称"));

        Button delUnitBtn = new Button("×");
        delUnitBtn.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-radius: 4px; -fx-padding: 6px 12px;");
        delUnitBtn.setOnAction(e -> {
            container.getChildren().remove(unitBox);
        });

        HBox.setHgrow(unitName, Priority.ALWAYS);
        titleBox.getChildren().addAll(unitLabel, unitName, delUnitBtn);

        VBox systemsBox = new VBox(10);
        Button addSystemBtn = new Button("+ 添加系统");
        addSystemBtn.setStyle("-fx-background-color: #74b9ff; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-radius: 4px; -fx-padding: 8px 16px;");
        addSystemBtn.setOnAction(e -> {
            VBox systemBox = createSystemBlock(systemsBox);
            systemsBox.getChildren().add(systemBox);
        });

        unitBox.getChildren().addAll(titleBox, addSystemBtn, systemsBox);
        return unitBox;
    }

    /**
     * 创建系统块
     * @param systemsBox 系统容器
     * @return 系统块
     */
    private VBox createSystemBlock(VBox systemsBox) {
        VBox systemBox = new VBox(8);
        systemBox.setStyle("-fx-border-color: #dfe6e9; -fx-border-radius: 6px; -fx-background-color: white; -fx-padding: 12px; -fx-background-radius: 6px;");

        HBox titleBox = new HBox(10);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Label systemLabel = new Label("【系统】");
        systemLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2d3436;");

        TextField systemName = new TextField();
        systemName.setPromptText("系统名称（必填）");
        systemName.setStyle("-fx-font-size: 13px; -fx-background-color: white; -fx-border-color: #e9ecef; -fx-border-radius: 4px; -fx-padding: 6px;");
        systemName.setTooltip(new Tooltip("请输入系统名称"));

        Button delSystemBtn = new Button("×");
        delSystemBtn.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-radius: 4px; -fx-padding: 4px 10px; -fx-font-size: 12px;");
        delSystemBtn.setOnAction(e -> systemsBox.getChildren().remove(systemBox));

        HBox.setHgrow(systemName, Priority.ALWAYS);
        titleBox.getChildren().addAll(systemLabel, systemName, delSystemBtn);

        VBox vulnsBox = new VBox(8);
        Button addVulnBtn = new Button("+ 添加漏洞");
        addVulnBtn.setStyle("-fx-background-color: #a29bfe; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-radius: 4px; -fx-padding: 6px 14px; -fx-font-size: 13px;");
        addVulnBtn.setOnAction(e -> {
            HBox vulnRow = createVulnRow(vulnsBox);
            vulnsBox.getChildren().add(vulnRow);
        });

        systemBox.getChildren().addAll(titleBox, addVulnBtn, vulnsBox);
        return systemBox;
    }

    /**
     * 创建漏洞行
     * @param vulnsBox 漏洞容器
     * @return 漏洞行
     */
    private HBox createVulnRow(VBox vulnsBox) {
        return createVulnRow(vulnsBox, null);
    }

    /**
     * 创建漏洞行（重载版本，支持加载状态标记）
     * @param vulnsBox 漏洞容器
     * @param isLoadingRef 加载状态标记数组
     * @return 漏洞行
     */
    private HBox createVulnRow(VBox vulnsBox, boolean[] isLoadingRef) {
        HBox vulnRow = new HBox(8);
        vulnRow.setStyle("-fx-background-color: white; -fx-border-color: #e9ecef; -fx-border-radius: 4px; -fx-padding: 8px 8px 8px 12px;");
        vulnRow.setAlignment(Pos.CENTER); // 居中对齐，适应不同高度的组件

        // 漏洞名称输入框（支持自动完成）
        TextField name = new TextField();
        name.setPromptText("漏洞名称");
        name.setTooltip(new Tooltip("必填，输入可匹配漏洞库"));

        // 其他字段
        TextField desc = new TextField();
        desc.setPromptText("描述");

        TextField level = new TextField();
        level.setPromptText("等级");

        TextField harm = new TextField();
        harm.setPromptText("危害");

        // 修复建议字段 - 固定高度多行文本框，带滚动条
        TextArea fix = new TextArea();
        fix.setPromptText("修复建议");
        fix.setPrefHeight(60); // 固定高度，约等于3行文本
        fix.setMaxHeight(60); // 禁止自动调整高度
        fix.setMinHeight(60); // 强制固定高度
        fix.setWrapText(true); // 自动换行
        fix.setStyle("-fx-font-size: 12px; -fx-background-color: #fafbfc; -fx-border-color: #e9ecef; -fx-border-radius: 3px; -fx-padding: 4px; -fx-wrap-text: true; -fx-pref-width: 150px; -fx-max-width: 300px;");

        // 禁用自动高度调整，保持固定高度
        fix.setScrollTop(0); // 初始滚动到顶部

        // 添加内容变化监听，确保滚动条在需要时出现
        fix.textProperty().addListener((observable, oldValue, newValue) -> {
            // 当内容变化时，如果有很多行，自动滚动到底部（可选功能）
            if (newValue != null && newValue.split("\n").length > 3) {
                javafx.application.Platform.runLater(() -> {
                    fix.setScrollTop(Double.MAX_VALUE); // 滚动到底部
                });
            }
        });

        // 简单的tooltip显示完整内容
        fix.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) { // 失去焦点时
                String text = fix.getText();
                if (!text.trim().isEmpty() && text.length() > 50) {
                    fix.setTooltip(new Tooltip("完整内容：\n" + text));
                } else {
                    fix.setTooltip(null);
                }
            }
        });

        // 支持Ctrl+Home/Ctrl+End键快速导航
        fix.setOnKeyPressed(e -> {
            if (e.isControlDown()) {
                if (e.getCode() == KeyCode.HOME) {
                    fix.setScrollTop(0);
                    e.consume();
                } else if (e.getCode() == KeyCode.END) {
                    fix.setScrollTop(Double.MAX_VALUE);
                    e.consume();
                }
            }
        });

        TextField repaired = new TextField();
        repaired.setPromptText("是否修复");

        // 统一样式设置
        String fieldStyle = "-fx-font-size: 12px; -fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 3px; -fx-padding: 5px;";

        // 应用统一样式
        name.setStyle(fieldStyle);
        desc.setStyle(fieldStyle);
        level.setStyle(fieldStyle);
        harm.setStyle(fieldStyle);
        repaired.setStyle(fieldStyle);

        // 设置字段最小宽度
        name.setMinWidth(80);
        desc.setMinWidth(80);
        level.setMinWidth(80);
        harm.setMinWidth(80);
        repaired.setMinWidth(60);
        repaired.setMaxWidth(70);

        // 设置修复建议的固定宽度，避免影响其他字段
        fix.setPrefWidth(150);
        fix.setMaxWidth(300); // 最大宽度限制

        // 如果提供了加载状态标记，使用它；否则创建新的
        final boolean[] isLoading = isLoadingRef != null ? isLoadingRef : new boolean[]{false};

        // 创建自定义工具提示作为建议列表
        CustomTooltip suggestionTooltip = new CustomTooltip();

        // 自动完成功能实现
        name.textProperty().addListener((observable, oldValue, newValue) -> {
            // 如果正在加载数据，不触发自动完成
            if (isLoading[0]) {
                return;
            }
            if (newValue == null || newValue.trim().isEmpty()) {
                suggestionTooltip.hide();
                return;
            }

            // 从漏洞库中匹配漏洞名称
            List<String> matchedVulns = vulnerabilityService.getVulnerabilityMap().keySet().stream()
                    .filter(vulnName -> vulnName.toLowerCase().contains(newValue.toLowerCase().trim()))
                    .sorted()
                    .collect(Collectors.toList());

            if (matchedVulns.isEmpty()) {
                suggestionTooltip.hide();
                return;
            }

            // 如果只有一个精确匹配，自动填充
            if (matchedVulns.size() == 1 &&
                matchedVulns.get(0).equalsIgnoreCase(newValue.trim())) {
                VulnerabilityService.Vulnerability vuln = vulnerabilityService.getVulnerability(matchedVulns.get(0));
                if (vuln != null) {
                    desc.setText(vuln.getDescription());
                    level.setText(vuln.getRiskLevel());
                    harm.setText(vuln.getHarm());
                    fix.setText(vuln.getSuggustion());
                }
                suggestionTooltip.hide();
                return;
            }

            // 显示建议列表
            suggestionTooltip.showSuggestions(matchedVulns, name, vuln -> {
                VulnerabilityService.Vulnerability vulnData = vulnerabilityService.getVulnerability(vuln);
                if (vulnData != null) {
                    name.setText(vuln);
                    desc.setText(vulnData.getDescription());
                    level.setText(vulnData.getRiskLevel());
                    harm.setText(vulnData.getHarm());
                    fix.setText(vulnData.getSuggustion());
                }
                suggestionTooltip.hide();
            });
        });

        // 当输入框失去焦点时隐藏建议
        name.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                suggestionTooltip.hide();
            }
        });

        // 当用户按下回车键时选择第一个匹配项
        name.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                suggestionTooltip.selectFirst();
                e.consume();
            }
        });

        Button delVulnBtn = new Button("×");
        delVulnBtn.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-radius: 50%; -fx-padding: 4px 6px; -fx-font-size: 11px; -fx-pref-width: 20px; -fx-pref-height: 20px; -fx-min-width: 20px; -fx-min-height: 20px;");
        delVulnBtn.setOnAction(e -> vulnsBox.getChildren().remove(vulnRow));

        vulnRow.getChildren().addAll(name, desc, level, harm, fix, repaired, delVulnBtn);

        // 设置布局权重，确保X按钮有足够空间
        HBox.setHgrow(name, Priority.ALWAYS);
        HBox.setHgrow(desc, Priority.ALWAYS);
        HBox.setHgrow(level, Priority.ALWAYS);
        HBox.setHgrow(harm, Priority.ALWAYS);
        HBox.setHgrow(fix, Priority.ALWAYS);
        HBox.setHgrow(repaired, Priority.NEVER);
        HBox.setHgrow(delVulnBtn, Priority.NEVER);

        // 设置最小宽度
        fix.setMinWidth(100);
        delVulnBtn.setMaxWidth(25);

        return vulnRow;
    }

    /**
     * 从UI收集数据并校验
     * @param container 容器
     * @return 是否校验成功
     */
    private boolean collectDataFromUI(VBox container) {
        unitEntries.clear();

        clearErrorStyles(container); // 校验前清除样式
        String errorStyle = "-fx-border-color: red; -fx-border-width: 2px;";

        Set<String> unitNameSet = new HashSet<>();

        for (Node node : container.getChildren()) {
            if (!(node instanceof VBox)) continue;
            VBox unitBox = (VBox) node;

            // 单位名称输入框位置
            HBox titleBox = (HBox) unitBox.getChildren().get(0);
            TextField unitNameField = (TextField) titleBox.getChildren().get(1);
            String unitName = unitNameField.getText().trim();

            if (unitName.isEmpty()) {
                unitNameField.setStyle(errorStyle);
                showAlert(Alert.AlertType.ERROR, "校验错误", "单位名称不能为空");
                return false;
            }

            if (unitNameSet.contains(unitName)) {
                unitNameField.setStyle(errorStyle);
                showAlert(Alert.AlertType.ERROR, "校验错误", "存在重复单位名称：" + unitName);
                return false;
            }

            unitNameSet.add(unitName);

            UnitEntry unit = new UnitEntry();
            unit.unit = unitName;
            unit.systems = new ArrayList<>();

            Set<String> systemNameSet = new HashSet<>();

            VBox systemsBox = (VBox) unitBox.getChildren().get(2);
            for (Node sNode : systemsBox.getChildren()) {
                if (!(sNode instanceof VBox)) continue;
                VBox systemBox = (VBox) sNode;

                HBox sysTitleBox = (HBox) systemBox.getChildren().get(0);
                TextField systemNameField = (TextField) sysTitleBox.getChildren().get(1);
                String systemName = systemNameField.getText().trim();

                if (systemName.isEmpty()) {
                    systemNameField.setStyle(errorStyle);
                    showAlert(Alert.AlertType.ERROR, "校验错误", "系统名称不能为空");
                    return false;
                }

                if (systemNameSet.contains(systemName)) {
                    systemNameField.setStyle(errorStyle);
                    showAlert(Alert.AlertType.ERROR, "校验错误", "单位 '" + unitName + "' 中存在重复系统名称：" + systemName);
                    return false;
                }

                systemNameSet.add(systemName);

                SystemEntry system = new SystemEntry();
                system.system = systemName;
                system.vulns = new ArrayList<>();

                VBox vulnsBox = (VBox) systemBox.getChildren().get(2);
                for (Node vNode : vulnsBox.getChildren()) {
                    if (!(vNode instanceof HBox)) continue;
                    HBox vulnRow = (HBox) vNode;

                    // 直接提取所有字段内容，包括TextArea
                    String vulnName = null, vulnDesc = null, vulnLevel = null, vulnHarm = null, vulnFix = null, vulnRepaired = null;

                    for (int i = 0; i < vulnRow.getChildren().size(); i++) {
                        Node n = vulnRow.getChildren().get(i);
                        if (i == 0 && n instanceof TextField) {
                            vulnName = ((TextField) n).getText().trim();
                        } else if (i == 1 && n instanceof TextField) {
                            vulnDesc = ((TextField) n).getText();
                        } else if (i == 2 && n instanceof TextField) {
                            vulnLevel = ((TextField) n).getText();
                        } else if (i == 3 && n instanceof TextField) {
                            vulnHarm = ((TextField) n).getText();
                        } else if (i == 4 && n instanceof TextArea) {
                            vulnFix = ((TextArea) n).getText(); // 直接获取TextArea内容，保持换行符
                        } else if (i == 5 && n instanceof TextField) {
                            vulnRepaired = ((TextField) n).getText();
                        }
                    }

                    if (vulnName == null || vulnName.isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "校验错误", "漏洞名称不能为空");
                        return false;
                    }

                    Vuln vuln = new Vuln();
                    vuln.name = vulnName;
                    vuln.desc = vulnDesc != null ? vulnDesc : "";
                    vuln.level = vulnLevel != null ? vulnLevel : "";
                    vuln.harm = vulnHarm != null ? vulnHarm : "";
                    vuln.fix = vulnFix != null ? vulnFix : ""; // TextArea内容，包含换行符
                    vuln.repaired = vulnRepaired != null ? vulnRepaired : "";

                    system.vulns.add(vuln);
                }

                unit.systems.add(system);

                if (system.vulns.isEmpty()) {
                    systemNameField.setStyle(errorStyle);
                    showAlert(Alert.AlertType.ERROR, "校验错误", "系统 '" + systemName + "' 必须至少有一个漏洞");
                    return false;
                }
            }

            unitEntries.add(unit);

            if (unit.systems.isEmpty()) {
                unitNameField.setStyle(errorStyle);
                showAlert(Alert.AlertType.ERROR, "校验错误", "单位 '" + unitName + "' 必须至少有一个系统");
                return false;
            }
        }

        return true;
    }

    /**
     * 保存漏洞数据
     */
    private void saveData() {
        try (FileOutputStream out = new FileOutputStream(saveFile)) {
            String json = JSON.toJSONString(unitEntries);
            out.write(json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            LogUtils.error(VulnTreeInputWindow.class, "保存文件失败：" + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "保存失败", e.getMessage());
        }
    }

    /**
     * 读取数据加载到UI
     * @param container 容器
     */
    private void loadDataToUI(VBox container) {
        if (!saveFile.exists()) return;

        // 创建加载状态标记
        boolean[] isLoading = {true};

        try (FileInputStream in = new FileInputStream(saveFile);
             Scanner scanner = new Scanner(in, "UTF-8").useDelimiter("\\A"))
        {
            String json = scanner.hasNext() ? scanner.next() : "";
            List<UnitEntry> units = JSON.parseArray(json, UnitEntry.class);
            for (UnitEntry unit : units) {
                VBox unitBox = createUnitBlock(container);
                HBox unitTitleBox = (HBox) unitBox.getChildren().get(0);
                TextField unitField = (TextField) unitTitleBox.getChildren().get(1);
                VBox systemsBox = (VBox) unitBox.getChildren().get(2);

                unitField.setText(unit.unit);
                for (SystemEntry sys : unit.systems) {
                    VBox sysBox = createSystemBlock(systemsBox);
                    HBox sysTitleBox = (HBox) sysBox.getChildren().get(0);
                    TextField sysField = (TextField) sysTitleBox.getChildren().get(1);
                    VBox vulnsBox = (VBox) sysBox.getChildren().get(2);

                    sysField.setText(sys.system);
                    for (Vuln vuln : sys.vulns) {
                        HBox vulnRow = createVulnRow(vulnsBox, isLoading);

                        ((TextField) vulnRow.getChildren().get(0)).setText(vuln.name);

                        ((TextField) vulnRow.getChildren().get(1)).setText(vuln.desc);
                        ((TextField) vulnRow.getChildren().get(2)).setText(vuln.level);
                        ((TextField) vulnRow.getChildren().get(3)).setText(vuln.harm);

                        // 处理修复建议的TextArea，确保换行符正确加载
                        TextArea fixField = (TextArea) vulnRow.getChildren().get(4);
                        fixField.setText(vuln.fix); // 直接设置完整内容，包含换行符

                        ((TextField) vulnRow.getChildren().get(5)).setText(vuln.repaired);

                        vulnsBox.getChildren().add(vulnRow);
                    }
                    systemsBox.getChildren().add(sysBox);
                }
                container.getChildren().add(unitBox);
            }
        } catch (IOException e) {
            LogUtils.error(VulnTreeInputWindow.class, "加载文件失败：" + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "加载失败", e.getMessage());
        }

        // 加载完成，重置标记
        isLoading[0] = false;
    }

    /**
     * 弹出报错框
     * @param type 弹窗类型
     * @param title 弹窗标题
     * @param content 弹窗内容
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * 清楚报错红框
     * @param root 要清楚的组件
     */
    private void clearErrorStyles(Pane root) {
        for (Node node : root.getChildren()) {
            if (node instanceof TextField) {
                node.setStyle("");
            } else if (node instanceof Pane) {
                clearErrorStyles((Pane) node); // 递归清除
            }
        }
    }

    /**
     * 自定义工具提示类，用于显示建议列表
     */
    private static class CustomTooltip {
        private Tooltip tooltip;
        private ListView<String> listView;
        private Consumer<String> onSelect;

        public CustomTooltip() {
            listView = new ListView<>();
            listView.setPrefHeight(120);
            listView.setPrefWidth(250);
            listView.setCellFactory(param -> {
                ListCell<String> cell = new ListCell<String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(item);
                        }
                    }
                };
                cell.setOnMouseClicked(e -> {
                    if (e.getClickCount() == 1 && !cell.isEmpty()) {
                        if (onSelect != null) {
                            onSelect.accept(cell.getItem());
                        }
                    }
                });
                return cell;
            });

            tooltip = new Tooltip();
            tooltip.setGraphic(listView);
            tooltip.setShowDelay(Duration.millis(100));
            tooltip.setHideDelay(Duration.millis(0));
            tooltip.setShowDuration(Duration.seconds(30));
        }

        public void showSuggestions(List<String> suggestions, TextField owner, Consumer<String> onSelect) {
            this.onSelect = onSelect;
            listView.getItems().clear();
            listView.getItems().addAll(suggestions);

            // 显示工具提示
            Point2D location = owner.localToScreen(0, owner.getHeight());
            if (location != null) {
                tooltip.show(owner, location.getX(), location.getY());
            }
        }

        public void hide() {
            tooltip.hide();
        }

        public void selectFirst() {
            if (!listView.getItems().isEmpty() && onSelect != null) {
                onSelect.accept(listView.getItems().get(0));
            }
        }
    }

}
