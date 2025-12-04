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
import org.scenicview.ScenicView;

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
        // 注释掉图标加载，避免文件不存在的错误
        // stage.getIcons().add(new javafx.scene.image.Image("/icon.png"));

        // 简洁纯色背景
        root.setStyle("-fx-background-color: #f5f6fa;");
        root.setPadding(new Insets(25));

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-border-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // 注释掉动态滚动条样式设置，避免NullPointerException
        // 滚动条样式可以通过CSS文件设置，或者保持默认样式
        /*
        scrollPane.lookup(".scroll-bar").setStyle(
            "-fx-background-color: transparent;" +
            "-fx-background-insets: 0;" +
            "-fx-padding: 0;"
        );
        scrollPane.lookup(".scroll-bar:vertical").setStyle(
            "-fx-background-color: rgba(255,255,255,0.2);" +
            "-fx-background-insets: 0;" +
            "-fx-pref-width: 8px;" +
            "-fx-border-radius: 4px;"
        );
        scrollPane.lookup(".scroll-bar:vertical .thumb").setStyle(
            "-fx-background-color: rgba(255,255,255,0.6);" +
            "-fx-background-insets: 2px;" +
            "-fx-border-radius: 3px;"
        );
        */

        VBox container = new VBox(20);
        container.setPadding(new Insets(20));
        container.setStyle("-fx-background-color: transparent; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-focus-traversable: false;");
        container.setFocusTraversable(false); // 禁用焦点，避免蓝色边框

        // 强制禁用容器内所有子容器的焦点效果
        container.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                container.setStyle("-fx-background-color: transparent; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-focus-traversable: false;");
                container.setFocusTraversable(false);
                // 递归禁用所有子容器的焦点
                disableFocusForChildren(container);
            }
        });
        scrollPane.setContent(container);

        // 增强反馈感按钮样式
        String primaryBtnStyle = "-fx-background-color: #4361ee; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 8px; -fx-padding: 12px 24px; -fx-font-size: 14px; -fx-cursor: hand; -fx-border-width: 2px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(67, 97, 238, 0.3), 6, 0, 0, 2);";
        String dangerBtnStyle = "-fx-background-color: #f53e57; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 8px; -fx-padding: 12px 24px; -fx-font-size: 14px; -fx-cursor: hand; -fx-border-width: 2px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(245, 62, 87, 0.3), 6, 0, 0, 2);";
        String successBtnStyle = "-fx-background-color: #26de81; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 8px; -fx-padding: 12px 24px; -fx-font-size: 14px; -fx-cursor: hand; -fx-border-width: 2px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(38, 222, 129, 0.3), 6, 0, 0, 2);";

        // 按钮状态样式 - 移除缩放，只保留轻微的按压感
        String primaryBtnHover = "-fx-background-color: #3651de; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 8px; -fx-padding: 12px 24px; -fx-font-size: 14px; -fx-cursor: hand; -fx-border-width: 2px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(67, 97, 238, 0.5), 8, 0, 0, 3);";
        String dangerBtnHover = "-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 8px; -fx-padding: 12px 24px; -fx-font-size: 14px; -fx-cursor: hand; -fx-border-width: 2px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(245, 62, 87, 0.5), 8, 0, 0, 3);";
        String successBtnHover = "-fx-background-color: #1eb980; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 8px; -fx-padding: 12px 24px; -fx-font-size: 14px; -fx-cursor: hand; -fx-border-width: 2px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(38, 222, 129, 0.5), 8, 0, 0, 3);";

        Button addUnitBtn = new Button("添加单位");
        addUnitBtn.setStyle(primaryBtnStyle);
        addUnitBtn.setOnAction(e -> {
            VBox unitBox = createUnitBlock(container);
            container.getChildren().add(unitBox);
            // 确保新创建的单位块也禁用焦点效果
            disableFocusForChildren(container);
        });

        // 增强悬停效果
        addUnitBtn.setOnMouseEntered(e -> {
            addUnitBtn.setStyle(primaryBtnHover);
        });
        addUnitBtn.setOnMouseExited(e -> {
            addUnitBtn.setStyle(primaryBtnStyle);
        });
        addUnitBtn.setOnMousePressed(e -> {
            addUnitBtn.setStyle(primaryBtnHover + " -fx-translate-y: 0px;");
        });
        addUnitBtn.setOnMouseReleased(e -> {
            addUnitBtn.setStyle(primaryBtnHover);
        });

        Button clearBtn = new Button("清空");
        clearBtn.setStyle(dangerBtnStyle);
        clearBtn.setOnAction(e -> {
            container.getChildren().clear();
            unitEntries.clear();
        });

        // 清空按钮悬停效果
        clearBtn.setOnMouseEntered(e -> {
            clearBtn.setStyle(dangerBtnHover);
        });
        clearBtn.setOnMouseExited(e -> {
            clearBtn.setStyle(dangerBtnStyle);
        });
        clearBtn.setOnMousePressed(e -> {
            clearBtn.setStyle(dangerBtnHover + " -fx-translate-y: 0px;");
        });
        clearBtn.setOnMouseReleased(e -> {
            clearBtn.setStyle(dangerBtnHover);
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

        // 保存按钮悬停效果
        saveBtn.setOnMouseEntered(e -> {
            saveBtn.setStyle(successBtnHover);
        });
        saveBtn.setOnMouseExited(e -> {
            saveBtn.setStyle(successBtnStyle);
        });
        saveBtn.setOnMousePressed(e -> {
            saveBtn.setStyle(successBtnHover + " -fx-translate-y: 0px;");
        });
        saveBtn.setOnMouseReleased(e -> {
            saveBtn.setStyle(successBtnHover);
        });

        // 简洁按钮容器样式
        HBox btnBox = new HBox(15, addUnitBtn, clearBtn, saveBtn);
        btnBox.setAlignment(Pos.CENTER);
        btnBox.setPadding(new Insets(0, 0, 25, 0));
        btnBox.setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-border-color: transparent;");
        btnBox.setFocusTraversable(false);

        root.getChildren().addAll(btnBox, scrollPane);

        loadDataToUI(container);

        // 递归禁用所有容器的焦点效果
        disableFocusForChildren(container);

        Scene scene = new Scene(root, 1200, 650);

        // 使用根节点样式设置，避免全局选择器导致的资源警告
        scene.getRoot().setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        // ScenicView.show(scene); // 临时注释掉调试工具，可能影响渲染
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
        unitBox.setStyle("-fx-border-color: #dfe6e9; -fx-border-radius: 8px; -fx-background-color: white; -fx-padding: 15px; -fx-background-radius: 8px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        unitBox.setFocusTraversable(false); // 禁用焦点，避免蓝色边框

        HBox titleBox = new HBox(12);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        // 简洁标签样式
        Label unitLabel = new Label("单位");
        unitLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2d3436;");

        // 简洁输入框样式
        TextField unitName = new TextField();
        unitName.setPromptText("单位名称（必填）");
        unitName.setStyle("-fx-font-size: 14px; -fx-background-color: #f8f9fa; -fx-border-color: #e9ecef; -fx-border-radius: 4px; -fx-padding: 8px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        unitName.setTooltip(new Tooltip("请输入单位名称"));

        // 阻止焦点冒泡到父容器
        unitName.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                // 当输入框获得焦点时，手动设置父容器不显示焦点边框
                unitBox.setStyle("-fx-border-color: #dfe6e9; -fx-border-radius: 8px; -fx-background-color: white; -fx-padding: 15px; -fx-background-radius: 8px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
            }
        });

        // 简洁删除按钮
        Button delUnitBtn = new Button("×");
        String delUnitBtnStyle = "-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-radius: 4px; -fx-padding: 6px 12px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(255, 107, 107, 0.3), 3, 0, 0, 1);";
        String delUnitBtnHover = "-fx-background-color: #ee5a52; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-radius: 4px; -fx-padding: 6px 12px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(255, 107, 107, 0.5), 5, 0, 0, 2);";
        delUnitBtn.setStyle(delUnitBtnStyle);
        delUnitBtn.setOnAction(e -> {
            container.getChildren().remove(unitBox);
        });

        // 删除按钮悬停效果
        delUnitBtn.setOnMouseEntered(e -> delUnitBtn.setStyle(delUnitBtnHover));
        delUnitBtn.setOnMouseExited(e -> delUnitBtn.setStyle(delUnitBtnStyle));

        HBox.setHgrow(unitName, Priority.ALWAYS);
        titleBox.getChildren().addAll(unitLabel, unitName, delUnitBtn);

        VBox systemsBox = new VBox(15);

        unitBox.getChildren().addAll(titleBox, systemsBox);

        // 简洁添加系统按钮容器
        HBox addSystemBtnContainer = new HBox();
        addSystemBtnContainer.setAlignment(Pos.CENTER);
        addSystemBtnContainer.setPadding(new Insets(10, 0, 0, 0));

        // 美化系统按钮样式
        String systemBtnStyle = "-fx-background-color: #74b9ff; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 6px; -fx-padding: 10px 18px; -fx-font-size: 13px; -fx-cursor: hand; -fx-border-width: 2px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(116, 185, 255, 0.3), 4, 0, 0, 2);";
        String systemBtnHover = "-fx-background-color: #5ba3f5; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 6px; -fx-padding: 10px 18px; -fx-font-size: 13px; -fx-cursor: hand; -fx-border-width: 2px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(116, 185, 255, 0.5), 6, 0, 0, 3);";

        Button addSystemBtn = new Button("+ 添加系统");
        addSystemBtn.setStyle(systemBtnStyle);
        addSystemBtn.setOnAction(e -> {
            VBox systemBox = createSystemBlock(systemsBox);
            systemsBox.getChildren().add(systemBox);
            // 确保新创建的系统块也禁用焦点效果
            disableFocusForChildren(systemsBox);
        });

        // 添加系统按钮悬停效果
        addSystemBtn.setOnMouseEntered(e -> {
            addSystemBtn.setStyle(systemBtnHover);
        });
        addSystemBtn.setOnMouseExited(e -> {
            addSystemBtn.setStyle(systemBtnStyle);
        });
        addSystemBtn.setOnMousePressed(e -> {
            addSystemBtn.setStyle(systemBtnHover + " -fx-translate-y: 0px;");
        });
        addSystemBtn.setOnMouseReleased(e -> {
            addSystemBtn.setStyle(systemBtnHover);
        });
        addSystemBtnContainer.getChildren().add(addSystemBtn);

        // 将添加系统按钮添加到单位块底部
        unitBox.getChildren().add(addSystemBtnContainer);
        return unitBox;
    }

    /**
     * 创建系统块
     * @param systemsBox 系统容器
     * @return 系统块
     */
    private VBox createSystemBlock(VBox systemsBox) {
        VBox systemBox = new VBox(8);
        systemBox.setStyle("-fx-border-color: #dfe6e9; -fx-border-radius: 6px; -fx-background-color: white; -fx-padding: 12px; -fx-background-radius: 6px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        systemBox.setFocusTraversable(false); // 禁用焦点，避免蓝色边框

        HBox titleBox = new HBox(10);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        // 简洁系统标签
        Label systemLabel = new Label("系统");
        systemLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2d3436;");

        // 简洁系统名称输入框
        TextField systemName = new TextField();
        systemName.setPromptText("系统名称（必填）");
        systemName.setStyle("-fx-font-size: 13px; -fx-background-color: white; -fx-border-color: #e9ecef; -fx-border-radius: 4px; -fx-padding: 6px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        systemName.setTooltip(new Tooltip("请输入系统名称"));

        // 阻止焦点冒泡到父容器
        systemName.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                // 当输入框获得焦点时，手动设置父容器不显示焦点边框
                systemBox.setStyle("-fx-border-color: #dfe6e9; -fx-border-radius: 6px; -fx-background-color: white; -fx-padding: 12px; -fx-background-radius: 6px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
            }
        });

        // 简洁系统删除按钮
        Button delSystemBtn = new Button("×");
        String delSystemBtnStyle = "-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-radius: 4px; -fx-padding: 4px 10px; -fx-font-size: 12px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(255, 107, 107, 0.3), 3, 0, 0, 1);";
        String delSystemBtnHover = "-fx-background-color: #ee5a52; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-radius: 4px; -fx-padding: 4px 10px; -fx-font-size: 12px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(255, 107, 107, 0.5), 5, 0, 0, 2);";
        delSystemBtn.setStyle(delSystemBtnStyle);
        delSystemBtn.setOnAction(e -> systemsBox.getChildren().remove(systemBox));

        // 系统删除按钮悬停效果
        delSystemBtn.setOnMouseEntered(e -> delSystemBtn.setStyle(delSystemBtnHover));
        delSystemBtn.setOnMouseExited(e -> delSystemBtn.setStyle(delSystemBtnStyle));

        HBox.setHgrow(systemName, Priority.ALWAYS);
        titleBox.getChildren().addAll(systemLabel, systemName, delSystemBtn);

        VBox vulnsBox = new VBox(12);

        systemBox.getChildren().addAll(titleBox, vulnsBox);

        // 简洁添加漏洞按钮容器
        HBox addVulnBtnContainer = new HBox();
        addVulnBtnContainer.setAlignment(Pos.CENTER);
        addVulnBtnContainer.setPadding(new Insets(8, 0, 0, 0));

        // 美化漏洞按钮样式
        String vulnBtnStyle = "-fx-background-color: #a29bfe; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 6px; -fx-padding: 8px 16px; -fx-font-size: 12px; -fx-cursor: hand; -fx-border-width: 2px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(162, 155, 254, 0.3), 4, 0, 0, 2);";
        String vulnBtnHover = "-fx-background-color: #8b7fea; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 6px; -fx-padding: 8px 16px; -fx-font-size: 12px; -fx-cursor: hand; -fx-border-width: 2px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(162, 155, 254, 0.5), 6, 0, 0, 3);";

        Button addVulnBtn = new Button("+ 添加漏洞");
        addVulnBtn.setStyle(vulnBtnStyle);
        addVulnBtn.setOnAction(e -> {
            HBox vulnRow = createVulnRow(vulnsBox);
            vulnsBox.getChildren().add(vulnRow);
            // 确保新创建的漏洞行也禁用焦点效果
            disableFocusForChildren(vulnsBox);
        });

        // 添加漏洞按钮悬停效果
        addVulnBtn.setOnMouseEntered(e -> {
            addVulnBtn.setStyle(vulnBtnHover);
        });
        addVulnBtn.setOnMouseExited(e -> {
            addVulnBtn.setStyle(vulnBtnStyle);
        });
        addVulnBtn.setOnMousePressed(e -> {
            addVulnBtn.setStyle(vulnBtnHover + " -fx-translate-y: 0px;");
        });
        addVulnBtn.setOnMouseReleased(e -> {
            addVulnBtn.setStyle(vulnBtnHover);
        });
        addVulnBtnContainer.getChildren().add(addVulnBtn);

        // 将添加漏洞按钮添加到系统块底部
        systemBox.getChildren().add(addVulnBtnContainer);
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
        HBox vulnRow = new HBox(6);
        vulnRow.setStyle("-fx-background-color: white; -fx-border-color: #e9ecef; -fx-border-radius: 4px; -fx-padding: 10px 6px 10px 12px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        vulnRow.setAlignment(Pos.CENTER_LEFT); // 改为左对齐，适应不同高度的组件
        vulnRow.setFocusTraversable(false); // 禁用焦点，避免蓝色边框

        // 漏洞名称输入框（支持自动完成）
        TextField name = new TextField();
        name.setPromptText("漏洞名称");
        name.setTooltip(new Tooltip("必填，输入可匹配漏洞库"));

        // 其他字段
        TextArea desc = new TextArea();
        desc.setPromptText("描述");
        desc.setPrefHeight(50); // 固定高度，约等于2行文本
        desc.setMaxHeight(50); // 禁止自动调整高度
        desc.setMinHeight(50); // 强制固定高度
        desc.setWrapText(true); // 自动换行

        TextField level = new TextField();
        level.setPromptText("等级");

        TextArea harm = new TextArea();
        harm.setPromptText("危害");
        harm.setPrefHeight(50); // 固定高度，约等于2行文本
        harm.setMaxHeight(50); // 禁止自动调整高度
        harm.setMinHeight(50); // 强制固定高度
        harm.setWrapText(true); // 自动换行

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

        // 为所有TextArea添加统一的交互功能
        setupTextAreaInteractions(fix, 3);
        setupTextAreaInteractions(desc, 2);
        setupTextAreaInteractions(harm, 2);

        TextField repaired = new TextField();
        repaired.setPromptText("是否修复");

        // 简洁统一输入框样式
        String fieldStyle = "-fx-font-size: 12px; -fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 3px; -fx-padding: 5px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;";

        // TextArea样式
        String areaStyle = "-fx-font-size: 12px; -fx-background-color: #fafbfc; -fx-border-color: #e9ecef; -fx-border-radius: 3px; -fx-padding: 4px; -fx-wrap-text: true; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;";

        // 应用统一样式
        name.setStyle(fieldStyle);
        desc.setStyle(areaStyle);
        level.setStyle(fieldStyle);
        harm.setStyle(areaStyle);
        repaired.setStyle(fieldStyle);

        // 为所有输入框添加焦点控制，防止父容器出现蓝色边框
        name.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                vulnRow.setStyle("-fx-background-color: white; -fx-border-color: #e9ecef; -fx-border-radius: 4px; -fx-padding: 8px 8px 8px 12px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
            }
        });
        desc.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                vulnRow.setStyle("-fx-background-color: white; -fx-border-color: #e9ecef; -fx-border-radius: 4px; -fx-padding: 8px 8px 8px 12px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
            }
        });
        level.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                vulnRow.setStyle("-fx-background-color: white; -fx-border-color: #e9ecef; -fx-border-radius: 4px; -fx-padding: 8px 8px 8px 12px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
            }
        });
        harm.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                vulnRow.setStyle("-fx-background-color: white; -fx-border-color: #e9ecef; -fx-border-radius: 4px; -fx-padding: 8px 8px 8px 12px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
            }
        });
        repaired.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                vulnRow.setStyle("-fx-background-color: white; -fx-border-color: #e9ecef; -fx-border-radius: 4px; -fx-padding: 8px 8px 8px 12px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
            }
        });

        // 为TextArea也添加焦点控制
        fix.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                vulnRow.setStyle("-fx-background-color: white; -fx-border-color: #e9ecef; -fx-border-radius: 4px; -fx-padding: 8px 8px 8px 12px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
            }
        });

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

        // 简洁漏洞删除按钮
        Button delVulnBtn = new Button("×");
        String delVulnBtnStyle = "-fx-background-color: #ff6b6b; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-radius: 50%; -fx-padding: 4px 6px; -fx-font-size: 11px; -fx-pref-width: 20px; -fx-pref-height: 20px; -fx-max-width: 20px; -fx-max-height: 20px; -fx-min-width: 20px; -fx-min-height: 20px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(255, 107, 107, 0.3), 3, 0, 0, 1);";
        String delVulnBtnHover = "-fx-background-color: #ee5a52; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-radius: 50%; -fx-padding: 4px 6px; -fx-font-size: 11px; -fx-pref-width: 20px; -fx-pref-height: 20px; -fx-max-width: 20px; -fx-max-height: 20px; -fx-min-width: 20px; -fx-min-height: 20px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(255, 107, 107, 0.5), 5, 0, 0, 2);";
        delVulnBtn.setStyle(delVulnBtnStyle);
        delVulnBtn.setOnAction(e -> vulnsBox.getChildren().remove(vulnRow));

        // 漏洞删除按钮悬停效果
        delVulnBtn.setOnMouseEntered(e -> delVulnBtn.setStyle(delVulnBtnHover));
        delVulnBtn.setOnMouseExited(e -> delVulnBtn.setStyle(delVulnBtnStyle));

        vulnRow.getChildren().addAll(name, desc, level, harm, fix, repaired, delVulnBtn);

        // 优化布局权重，确保字段合理分配空间
        HBox.setHgrow(name, Priority.NEVER); // 漏洞名称固定宽度
        HBox.setHgrow(desc, Priority.SOMETIMES); // 描述可扩展
        HBox.setHgrow(level, Priority.NEVER); // 等级固定宽度
        HBox.setHgrow(harm, Priority.SOMETIMES); // 危害可扩展
        HBox.setHgrow(fix, Priority.ALWAYS); // 修复建议最大权重
        HBox.setHgrow(repaired, Priority.NEVER); // 修复状态固定宽度
        HBox.setHgrow(delVulnBtn, Priority.NEVER);

        // 设置各字段的合适宽度
        name.setPrefWidth(120);
        name.setMaxWidth(150);
        level.setPrefWidth(60);
        level.setMaxWidth(80);
        repaired.setPrefWidth(70);
        repaired.setMaxWidth(90);
        desc.setMinWidth(100);
        desc.setPrefWidth(140);
        harm.setMinWidth(100);
        harm.setPrefWidth(140);
        fix.setMinWidth(150);
        delVulnBtn.setMaxWidth(30);
        delVulnBtn.setMinWidth(30);

        return vulnRow;
    }

    /**
     * 为TextArea设置统一的交互功能
     * @param textArea 文本区域
     * @param maxVisibleLines 最大可见行数
     */
    private void setupTextAreaInteractions(TextArea textArea, int maxVisibleLines) {
        // 添加内容变化监听，确保滚动条在需要时出现
        textArea.textProperty().addListener((observable, oldValue, newValue) -> {
            // 当内容变化时，如果有很多行，自动滚动到底部（可选功能）
            if (newValue != null && newValue.split("\n").length > maxVisibleLines) {
                javafx.application.Platform.runLater(() -> {
                    textArea.setScrollTop(Double.MAX_VALUE); // 滚动到底部
                });
            }
        });

        // 简单的tooltip显示完整内容
        textArea.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) { // 失去焦点时
                String text = textArea.getText();
                if (!text.trim().isEmpty() && text.length() > 30) {
                    textArea.setTooltip(new Tooltip("完整内容：\n" + text));
                } else {
                    textArea.setTooltip(null);
                }
            }
        });

        // 支持Ctrl+Home/Ctrl+End键快速导航
        textArea.setOnKeyPressed(e -> {
            if (e.isControlDown()) {
                if (e.getCode() == KeyCode.HOME) {
                    textArea.setScrollTop(0);
                    e.consume();
                } else if (e.getCode() == KeyCode.END) {
                    textArea.setScrollTop(Double.MAX_VALUE);
                    e.consume();
                }
            }
        });
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

            VBox systemsBox = (VBox) unitBox.getChildren().get(1);
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

                VBox vulnsBox = (VBox) systemBox.getChildren().get(1);
                for (Node vNode : vulnsBox.getChildren()) {
                    if (!(vNode instanceof HBox)) continue;
                    HBox vulnRow = (HBox) vNode;

                    // 直接提取所有字段内容，包括TextArea
                    String vulnName = null, vulnDesc = null, vulnLevel = null, vulnHarm = null, vulnFix = null, vulnRepaired = null;

                    for (int i = 0; i < vulnRow.getChildren().size(); i++) {
                        Node n = vulnRow.getChildren().get(i);
                        if (i == 0 && n instanceof TextField) {
                            vulnName = ((TextField) n).getText().trim();
                        } else if (i == 1 && n instanceof TextArea) {
                            vulnDesc = ((TextArea) n).getText(); // TextArea内容，保持换行符
                        } else if (i == 2 && n instanceof TextField) {
                            vulnLevel = ((TextField) n).getText();
                        } else if (i == 3 && n instanceof TextArea) {
                            vulnHarm = ((TextArea) n).getText(); // TextArea内容，保持换行符
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
                VBox systemsBox = (VBox) unitBox.getChildren().get(1);

                unitField.setText(unit.unit);
                for (SystemEntry sys : unit.systems) {
                    VBox sysBox = createSystemBlock(systemsBox);
                    HBox sysTitleBox = (HBox) sysBox.getChildren().get(0);
                    TextField sysField = (TextField) sysTitleBox.getChildren().get(1);
                    VBox vulnsBox = (VBox) sysBox.getChildren().get(1);

                    sysField.setText(sys.system);
                    for (Vuln vuln : sys.vulns) {
                        HBox vulnRow = createVulnRow(vulnsBox, isLoading);

                        ((TextField) vulnRow.getChildren().get(0)).setText(vuln.name);

                        // 处理描述的TextArea，确保换行符正确加载
                        TextArea descField = (TextArea) vulnRow.getChildren().get(1);
                        descField.setText(vuln.desc); // 直接设置完整内容，包含换行符

                        ((TextField) vulnRow.getChildren().get(2)).setText(vuln.level);

                        // 处理危害的TextArea，确保换行符正确加载
                        TextArea harmField = (TextArea) vulnRow.getChildren().get(3);
                        harmField.setText(vuln.harm); // 直接设置完整内容，包含换行符

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
     * 递归禁用所有子容器的焦点效果
     * @param parent 父容器
     */
    private void disableFocusForChildren(Pane parent) {
        for (Node child : parent.getChildren()) {
            if (child instanceof Pane) {
                Pane childPane = (Pane) child;
                childPane.setFocusTraversable(false);
                childPane.setStyle(childPane.getStyle() + "; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-focus-traversable: false;");
                disableFocusForChildren(childPane); // 递归处理
            } else if (child instanceof VBox || child instanceof HBox) {
                child.setFocusTraversable(false);
                child.setStyle(child.getStyle() + "; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-focus-traversable: false;");
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
            listView.setPrefHeight(150);
            listView.setPrefWidth(280);

            // 美化ListView样式
            listView.setStyle(
                "-fx-background-color: white;" +
                "-fx-border-color: #e9ecef;" +
                "-fx-border-radius: 8px;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.15), 8, 0, 0, 4);" +
                "-fx-background-insets: 0;" +
                "-fx-padding: 0;" +
                "-fx-focus-color: transparent;" +
                "-fx-faint-focus-color: transparent;"
            );
            listView.setCellFactory(param -> {
                ListCell<String> cell = new ListCell<String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                            setStyle(
                                "-fx-background-color: transparent;" +
                                "-fx-border-color: transparent;" +
                                "-fx-padding: 8px 12px;"
                            );
                        } else {
                            setText(item);
                            setStyle(
                                "-fx-background-color: transparent;" +
                                "-fx-border-color: transparent;" +
                                "-fx-text-fill: #2d3436;" +
                                "-fx-font-size: 13px;" +
                                "-fx-font-family: 'System';" +
                                "-fx-padding: 8px 12px;" +
                                "-fx-background-insets: 0;" +
                                "-fx-border-insets: 0;"
                            );
                        }
                    }
                };

                // 悬停效果
                cell.setOnMouseEntered(e -> {
                    if (!cell.isEmpty()) {
                        cell.setStyle(
                            "-fx-background-color: #f8f9fa;" +
                            "-fx-border-color: transparent;" +
                            "-fx-text-fill: #2d3436;" +
                            "-fx-font-size: 13px;" +
                            "-fx-font-family: 'System';" +
                            "-fx-padding: 8px 12px;" +
                            "-fx-background-insets: 0;" +
                            "-fx-border-insets: 0;" +
                            "-fx-cursor: hand;"
                        );
                    }
                });

                cell.setOnMouseExited(e -> {
                    if (!cell.isEmpty()) {
                        cell.setStyle(
                            "-fx-background-color: transparent;" +
                            "-fx-border-color: transparent;" +
                            "-fx-text-fill: #2d3436;" +
                            "-fx-font-size: 13px;" +
                            "-fx-font-family: 'System';" +
                            "-fx-padding: 8px 12px;" +
                            "-fx-background-insets: 0;" +
                            "-fx-border-insets: 0;"
                        );
                    }
                });

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
            tooltip.setShowDelay(Duration.millis(150));
            tooltip.setHideDelay(Duration.millis(0));
            tooltip.setShowDuration(Duration.seconds(30));

            // 美化Tooltip样式
            tooltip.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-border-color: transparent;" +
                "-fx-padding: 0;" +
                "-fx-background-insets: 0;"
            );
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
