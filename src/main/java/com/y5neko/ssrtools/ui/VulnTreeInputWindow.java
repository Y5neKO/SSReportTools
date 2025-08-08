package com.y5neko.ssrtools.ui;

import com.alibaba.fastjson2.JSON;
import com.y5neko.ssrtools.models.vulntree.SystemEntry;
import com.y5neko.ssrtools.models.vulntree.UnitEntry;
import com.y5neko.ssrtools.models.vulntree.Vuln;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.y5neko.ssrtools.config.GlobalConfig.VULN_TREE_PATH;

public class VulnTreeInputWindow {

    private final List<UnitEntry> unitEntries = new ArrayList<>();
    private final File saveFile = new File(VULN_TREE_PATH);
    private final VBox root = new VBox(10);

    /**
     * 显示漏洞录入窗口
     */
    public void show() {
        Stage stage = new Stage();
        stage.setTitle("漏洞录入");

        ScrollPane scrollPane = new ScrollPane();
        VBox container = new VBox(12);
        scrollPane.setContent(container);
        scrollPane.setFitToWidth(true);

        Button addUnitBtn = new Button("+ 添加单位");
        addUnitBtn.setStyle("-fx-background-color: #007acc; -fx-text-fill: white;");
        addUnitBtn.setOnAction(e -> {
            VBox unitBox = createUnitBlock(container);
            container.getChildren().add(unitBox);
        });

        Button clearBtn = new Button("清空");
        clearBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;"); // 红色按钮
        clearBtn.setOnAction(e -> {
            // 清空UI中的所有单位块
            container.getChildren().clear();
            // 清空内存中的数据
            unitEntries.clear();
        });

        Button saveBtn = new Button("保存");
        saveBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> {
            if (collectDataFromUI(container)) {
                saveData();
                showAlert(Alert.AlertType.INFORMATION, "保存成功", "数据已成功保存");
            }
        });

        root.setPadding(new Insets(15));
        // 顺序：添加单位，清空，保存按钮
        HBox btnBox = new HBox(10, addUnitBtn, clearBtn, saveBtn);
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
        VBox unitBox = new VBox(8);
        unitBox.setStyle("-fx-border-color: #999999; -fx-padding: 10; -fx-background-color: #f0f8ff;"); // 浅蓝背景

        HBox titleBox = new HBox(10);
        Label unitLabel = new Label("单位");
        unitLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
        TextField unitName = new TextField();
        unitName.setPromptText("单位名称");
        unitName.setTooltip(new Tooltip("必填"));
        Button delUnitBtn = new Button("删除");
        delUnitBtn.setStyle("-fx-background-color: #ff6666; -fx-text-fill: white;");
        delUnitBtn.setOnAction(e -> container.getChildren().remove(unitBox));
        HBox.setHgrow(unitName, Priority.ALWAYS);
        titleBox.getChildren().addAll(unitLabel, unitName, delUnitBtn);

        VBox systemsBox = new VBox(8);
        Button addSystemBtn = new Button("+ 添加系统");
        addSystemBtn.setStyle("-fx-background-color: #3399ff; -fx-text-fill: white;");
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
        systemBox.setStyle("-fx-border-color: #66a3ff; -fx-padding: 10; -fx-background-color: #e6f0ff;"); // 更浅蓝

        HBox titleBox = new HBox(10);
        Label systemLabel = new Label("系统");
        systemLabel.setStyle("-fx-font-weight: bold;");
        TextField systemName = new TextField();
        systemName.setPromptText("系统名称");
        systemName.setTooltip(new Tooltip("必填"));
        Button delSystemBtn = new Button("删除");
        delSystemBtn.setStyle("-fx-background-color: #ff6666; -fx-text-fill: white;");
        delSystemBtn.setOnAction(e -> systemsBox.getChildren().remove(systemBox));
        HBox.setHgrow(systemName, Priority.ALWAYS);
        titleBox.getChildren().addAll(systemLabel, systemName, delSystemBtn);

        VBox vulnsBox = new VBox(8);
        Button addVulnBtn = new Button("+ 添加漏洞");
        addVulnBtn.setStyle("-fx-background-color: #3399ff; -fx-text-fill: white;");
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
        HBox vulnRow = new HBox(8);

        TextField name = new TextField();
        name.setPromptText("漏洞名称");
        name.setTooltip(new Tooltip("必填"));

        TextField desc = new TextField();
        desc.setPromptText("描述");

        TextField level = new TextField();
        level.setPromptText("等级");

        TextField harm = new TextField();
        harm.setPromptText("危害");

        TextField fix = new TextField();
        fix.setPromptText("修复建议");

        TextField repaired = new TextField();
        repaired.setPromptText("是否修复(是/否)");

        // 自适应
        for (TextField tf : Arrays.asList(name, desc, level, harm, fix)) {
            HBox.setHgrow(tf, Priority.ALWAYS);
            tf.setMinWidth(50);
        }

        Button delVulnBtn = new Button("删除");
        delVulnBtn.setStyle("-fx-background-color: #ff6666; -fx-text-fill: white;");
        delVulnBtn.setOnAction(e -> vulnsBox.getChildren().remove(vulnRow));

        vulnRow.getChildren().addAll(name, desc, level, harm, fix, repaired, delVulnBtn);
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

                    List<TextField> fields = new ArrayList<>();
                    for (Node n : vulnRow.getChildren()) {
                        if (n instanceof TextField) {
                            fields.add((TextField) n);
                        }
                    }

                    if (fields.isEmpty()) continue;

                    String vulnName = fields.get(0).getText().trim();
                    if (vulnName.isEmpty()) {
                        fields.get(0).setStyle(errorStyle);
                        showAlert(Alert.AlertType.ERROR, "校验错误", "漏洞名称不能为空");
                        return false;
                    }

                    Vuln vuln = new Vuln();
                    vuln.name = vulnName;
                    vuln.desc = fields.size() > 1 ? fields.get(1).getText() : "";
                    vuln.level = fields.size() > 2 ? fields.get(2).getText() : "";
                    vuln.harm = fields.size() > 3 ? fields.get(3).getText() : "";
                    vuln.fix = fields.size() > 4 ? fields.get(4).getText() : "";
                    // 修复状态
                    vuln.repaired = fields.size() > 5 ? fields.get(5).getText() : "";

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
            System.out.println(e.getMessage());
            showAlert(Alert.AlertType.ERROR, "保存失败", e.getMessage());
        }
    }

    /**
     * 读取数据加载到UI
     * @param container 容器
     */
    private void loadDataToUI(VBox container) {
        if (!saveFile.exists()) return;
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
                        HBox vulnRow = createVulnRow(vulnsBox);

                        ((TextField) vulnRow.getChildren().get(0)).setText(vuln.name);
                        ((TextField) vulnRow.getChildren().get(1)).setText(vuln.desc);
                        ((TextField) vulnRow.getChildren().get(2)).setText(vuln.level);
                        ((TextField) vulnRow.getChildren().get(3)).setText(vuln.harm);
                        ((TextField) vulnRow.getChildren().get(4)).setText(vuln.fix);
                        ((TextField) vulnRow.getChildren().get(5)).setText(vuln.repaired);

                        vulnsBox.getChildren().add(vulnRow);
                    }
                    systemsBox.getChildren().add(sysBox);
                }
                container.getChildren().add(unitBox);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            showAlert(Alert.AlertType.ERROR, "加载失败", e.getMessage());
        }
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

}
