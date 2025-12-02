package com.y5neko.ssrtools.ui;

import com.y5neko.ssrtools.utils.LogUtils;
import com.y5neko.ssrtools.utils.MiscUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import static com.y5neko.ssrtools.config.GlobalConfig.VULN_WIKI_FILE_PATH;

/**
 * 漏洞库编辑器
 */
public class VulnEditorWindow {
    private Stage stage;

    private TableView<Vulnerability> table;
    private final ObservableList<Vulnerability> masterData = FXCollections.observableArrayList();
    private final FilteredList<Vulnerability> filteredData = new FilteredList<>(masterData, p -> true);

    private TextField filterField;
    private TextField nameField;
    private TextArea harmArea;
    private TextField riskField;
    private TextArea descArea;
    private TextArea suggestArea;

    private Button btnNew;
    private Button btnSave;
    private Button btnDelete;
    private Button btnLoad;
    private Button btnOverwrite;
    private Button btnExport;

    /**
     * 构造函数
     */
    public VulnEditorWindow() {
        initControls();
        stage = new Stage();
    }

    /**
     * 显示漏洞库编辑器窗口
     */
    public void show() {
        stage.setTitle("漏洞库编辑器");
        BorderPane root = layoutUI();
        bindEvents(stage);
        stage.setScene(new Scene(root, 1200, 700));
        stage.setMinWidth(1100);
        stage.setMinHeight(650);
        stage.show();
        loadDefaultVulnerabilities();
    }

    /**
     * 初始化控件
     */
    private void initControls() {
        filterField = new TextField();
        filterField.setPromptText("根据漏洞名称过滤...");
        filterField.setStyle("-fx-font-size: 14px; -fx-padding: 8px 12px; -fx-border-radius: 6px; -fx-border-color: #dfe6e9; -fx-border-width: 1px; -fx-background-radius: 6px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        table = new TableView<>();
        table.setPlaceholder(new Label("暂无漏洞"));
        table.setStyle("-fx-background-color: white; -fx-border-color: #dfe6e9; -fx-border-radius: 8px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        nameField = new TextField();
        nameField.setStyle("-fx-font-size: 13px; -fx-padding: 8px 10px; -fx-border-radius: 6px; -fx-border-color: #dfe6e9; -fx-border-width: 1px; -fx-background-radius: 6px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        harmArea = new TextArea();
        harmArea.setPrefRowCount(3);
        harmArea.setWrapText(true);
        harmArea.setStyle("-fx-font-size: 13px; -fx-padding: 8px 10px; -fx-border-radius: 6px; -fx-border-color: #dfe6e9; -fx-border-width: 1px; -fx-background-radius: 6px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        riskField = new TextField();
        riskField.setStyle("-fx-font-size: 13px; -fx-padding: 8px 10px; -fx-border-radius: 6px; -fx-border-color: #dfe6e9; -fx-border-width: 1px; -fx-background-radius: 6px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        descArea = new TextArea();
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);
        descArea.setStyle("-fx-font-size: 13px; -fx-padding: 8px 10px; -fx-border-radius: 6px; -fx-border-color: #dfe6e9; -fx-border-width: 1px; -fx-background-radius: 6px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        suggestArea = new TextArea();
        suggestArea.setPrefRowCount(3);
        suggestArea.setWrapText(true);
        suggestArea.setStyle("-fx-font-size: 13px; -fx-padding: 8px 10px; -fx-border-radius: 6px; -fx-border-color: #dfe6e9; -fx-border-width: 1px; -fx-background-radius: 6px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        btnNew = new Button("新增漏洞");
        btnSave = new Button("保存到列表");
        btnDelete = new Button("删除选中");
        btnLoad = new Button("加载 YAML");
        btnOverwrite = new Button("保存 YAML");
        btnExport = new Button("另存为 YAML");

        // 应用按钮样式
        applyButtonStyles();
    }

    /**
     * 应用按钮样式
     */
    private void applyButtonStyles() {
        // 主要按钮样式 - 减小padding和字体大小
        String primaryBtnStyle = "-fx-background-color: #4361ee; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 6px; -fx-padding: 8px 14px; -fx-font-size: 12px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(67, 97, 238, 0.2), 4, 0, 0, 1);";
        String primaryBtnHover = "-fx-background-color: #3651de; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 6px; -fx-padding: 8px 14px; -fx-font-size: 12px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(67, 97, 238, 0.4), 6, 0, 0, 2);";

        // 成功按钮样式
        String successBtnStyle = "-fx-background-color: #26de81; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 6px; -fx-padding: 8px 14px; -fx-font-size: 12px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(38, 222, 129, 0.2), 4, 0, 0, 1);";
        String successBtnHover = "-fx-background-color: #1eb980; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 6px; -fx-padding: 8px 14px; -fx-font-size: 12px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(38, 222, 129, 0.4), 6, 0, 0, 2);";

        // 危险按钮样式
        String dangerBtnStyle = "-fx-background-color: #f53e57; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 6px; -fx-padding: 8px 14px; -fx-font-size: 12px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(245, 62, 87, 0.2), 4, 0, 0, 1);";
        String dangerBtnHover = "-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 6px; -fx-padding: 8px 14px; -fx-font-size: 12px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(245, 62, 87, 0.4), 6, 0, 0, 2);";

        // 次要按钮样式
        String secondaryBtnStyle = "-fx-background-color: #74b9ff; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 6px; -fx-padding: 8px 14px; -fx-font-size: 12px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(116, 185, 255, 0.2), 4, 0, 0, 1);";
        String secondaryBtnHover = "-fx-background-color: #5ba3f5; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 6px; -fx-padding: 8px 14px; -fx-font-size: 12px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(116, 185, 255, 0.4), 6, 0, 0, 2);";

        // 应用到按钮
        btnNew.setStyle(primaryBtnStyle);
        btnSave.setStyle(successBtnStyle);
        btnDelete.setStyle(dangerBtnStyle);
        btnLoad.setStyle(secondaryBtnStyle);
        btnOverwrite.setStyle(successBtnStyle);
        btnExport.setStyle(secondaryBtnStyle);

        // 添加悬停效果
        addHoverEffect(btnNew, primaryBtnStyle, primaryBtnHover);
        addHoverEffect(btnSave, successBtnStyle, successBtnHover);
        addHoverEffect(btnDelete, dangerBtnStyle, dangerBtnHover);
        addHoverEffect(btnLoad, secondaryBtnStyle, secondaryBtnHover);
        addHoverEffect(btnOverwrite, successBtnStyle, successBtnHover);
        addHoverEffect(btnExport, secondaryBtnStyle, secondaryBtnHover);
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
        // 表格列配置 - 优化宽度分配
        TableColumn<Vulnerability, String> nameCol = new TableColumn<>("漏洞名称");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(180);
        nameCol.setMinWidth(150);
        nameCol.setMaxWidth(250);
        nameCol.setStyle("-fx-font-size: 14px; -fx-font-weight: 600;");
        nameCol.setResizable(true);

        TableColumn<Vulnerability, String> riskCol = new TableColumn<>("风险等级");
        riskCol.setCellValueFactory(new PropertyValueFactory<>("risklevel"));
        riskCol.setPrefWidth(100);
        riskCol.setMinWidth(80);
        riskCol.setMaxWidth(120);
        riskCol.setStyle("-fx-font-size: 14px; -fx-font-weight: 600;");
        riskCol.setResizable(true);

        TableColumn<Vulnerability, String> harmCol = new TableColumn<>("漏洞危害");
        harmCol.setCellValueFactory(cell -> {
            String v = cell.getValue().getHarm();
            if (v == null) v = "";
            // 将多行文本转换为单行显示，替换换行符为空格
            String singleLine = v.replace("\n", " ").replace("\r", " ").trim();
            // 清理多余空格
            singleLine = singleLine.replaceAll("\\s+", " ");
            // 增加显示长度限制，因为现在有更多空间
            String preview = singleLine.length() > 80 ? singleLine.substring(0, 77) + "..." : singleLine;
            return new SimpleStringProperty(preview);
        });
        harmCol.setPrefWidth(350);
        harmCol.setMinWidth(300);
        harmCol.setStyle("-fx-font-size: 14px; -fx-font-weight: 600;");
        harmCol.setResizable(true);

        table.getColumns().setAll(nameCol, riskCol, harmCol);
        table.setStyle("-fx-background-color: white; -fx-border-color: #dfe6e9; -fx-border-radius: 8px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        // 使用智能调整策略，允许列拖动调整大小
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // 用过滤列表绑定表格
        table.setItems(filteredData);

        // 创建搜索区域
        Label searchLabel = new Label("搜索漏洞");
        searchLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 600; -fx-text-fill: #2d3436;");

        VBox searchBox = new VBox(8);
        searchBox.getChildren().addAll(searchLabel, filterField);
        searchBox.setPadding(new Insets(0, 0, 12, 0));

        VBox tableBox = new VBox(6);
        tableBox.setPadding(new Insets(12));
        tableBox.setStyle("-fx-background-color: #f8f9fa; -fx-border-radius: 8px; -fx-background-radius: 8px;");
        tableBox.getChildren().addAll(searchBox, table);

        // 设置表格垂直扩展
        VBox.setVgrow(table, Priority.ALWAYS);

        // 创建表单区域
        Label formTitle = new Label("漏洞详情编辑");
        formTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #2d3436;");

        // 创建字段标签样式
        Label nameLabel = new Label("漏洞名称");
        nameLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #636e72;");

        Label harmLabel = new Label("漏洞危害");
        harmLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #636e72;");

        Label riskLabel = new Label("风险等级");
        riskLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #636e72;");

        Label descLabel = new Label("漏洞描述");
        descLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #636e72;");

        Label suggestLabel = new Label("修复建议");
        suggestLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #636e72;");

        // 创建主要操作按钮行
        HBox primaryButtons = new HBox(8, btnNew, btnSave, btnDelete);
        primaryButtons.setAlignment(Pos.CENTER);
        primaryButtons.setPadding(new Insets(10, 0, 8, 0));
        primaryButtons.setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        // 创建文件操作按钮行
        HBox fileButtons = new HBox(8, btnLoad, btnOverwrite, btnExport);
        fileButtons.setAlignment(Pos.CENTER);
        fileButtons.setPadding(new Insets(0, 0, 10, 0));
        fileButtons.setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        // 创建表单容器
        VBox form = new VBox(8);
        form.setPadding(new Insets(15));
        form.setStyle("-fx-background-color: white; -fx-border-color: #dfe6e9; -fx-border-radius: 10px; -fx-background-radius: 10px;");
        form.getChildren().addAll(
                formTitle,
                nameLabel, nameField,
                harmLabel, harmArea,
                riskLabel, riskField,
                descLabel, descArea,
                suggestLabel, suggestArea,
                primaryButtons,
                fileButtons
        );

        VBox.setVgrow(harmArea, Priority.NEVER);
        VBox.setVgrow(descArea, Priority.ALWAYS);
        VBox.setVgrow(suggestArea, Priority.ALWAYS);

        // 设置根容器
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #f5f7fa;");

        // 创建左右分割容器
        HBox mainContent = new HBox(15);
        mainContent.getChildren().addAll(tableBox, form);
        HBox.setHgrow(tableBox, Priority.ALWAYS);
        HBox.setHgrow(form, Priority.NEVER);
        form.setPrefWidth(400); // 减小表单宽度，给表格更多空间
        form.setMaxWidth(420);  // 限制最大宽度

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

        // 过滤输入框监听
        filterField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = newVal == null ? "" : newVal.trim().toLowerCase();
            filteredData.setPredicate(vuln -> {
                if (filter.isEmpty()) return true;
                return vuln.getName().toLowerCase().contains(filter);
            });
        });

        // 按钮事件
        btnNew.setOnAction(e -> onNew());
        btnSave.setOnAction(e -> onSave());
        btnDelete.setOnAction(e -> onDelete());
        btnLoad.setOnAction(e -> onLoadYaml(stage));
        btnExport.setOnAction(e -> onSaveYaml(stage));
        btnOverwrite.setOnAction(e -> {
            onSave();
            onOverwriteYaml();
        });
    }

    /**
     * 加载默认漏洞库
     */
    private void loadDefaultVulnerabilities() {
        File file = new File(MiscUtils.getAbsolutePath(VULN_WIKI_FILE_PATH));
        if (!file.exists()) {
            System.out.println("默认漏洞库文件不存在：" + MiscUtils.getAbsolutePath(VULN_WIKI_FILE_PATH));
            return;
        }
        loadYaml(file);
    }

    /**
     * 加载YAML文件
     * @param file 文件
     */
    private void loadYaml(File file) {
        try (InputStream is = Files.newInputStream(file.toPath())) {
            Yaml yaml = new Yaml();
            Object obj = yaml.load(is);
            masterData.clear();
            if (obj instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) obj;
                Object vs = map.get("vulnerabilities");
                if (vs instanceof List) {
                    for (Object item : (List<?>) vs) {
                        if (item instanceof Map) {
                            masterData.add(Vulnerability.fromMap((Map<?, ?>) item));
                        }
                    }
                }
            } else if (obj instanceof List) {
                for (Object item : (List<?>) obj) {
                    if (item instanceof Map) {
                        masterData.add(Vulnerability.fromMap((Map<?, ?>) item));
                    }
                }
            }
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "加载失败", ex.getMessage());
            LogUtils.error(VulnEditorWindow.class, "加载失败" + ex.getMessage());
        }
    }

    /**
     * 保存YAML文件
     * @param file 文件
     */
    private void saveYaml(File file) {
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8)) {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);

            org.yaml.snakeyaml.representer.Representer repr = new org.yaml.snakeyaml.representer.Representer(options) {
                @Override
                protected org.yaml.snakeyaml.nodes.Node representScalar(
                        org.yaml.snakeyaml.nodes.Tag tag, String value, DumperOptions.ScalarStyle style) {
                    if ((this.getPropertyUtils().getProperties(Vulnerability.class).stream()
                            .anyMatch(p -> p.getName().equals("description") || p.getName().equals("suggustion")))
                            && value != null && value.contains("\n")) {
                        return super.representScalar(tag, value, DumperOptions.ScalarStyle.LITERAL);
                    }
                    return super.representScalar(tag, value, style);
                }
            };

            Yaml yaml = new Yaml(repr, options);

            Map<String, Object> root = new LinkedHashMap<>();
            List<Object> list = new ArrayList<>();
            for (Vulnerability v : masterData) {
                list.add(v.toMap());
            }
            root.put("vulnerabilities", list);

            yaml.dump(root, writer);
            showAlert(Alert.AlertType.INFORMATION, "保存成功", "漏洞库已保存至：" + file.getAbsolutePath());
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "保存失败", ex.getMessage());
            LogUtils.error(VulnEditorWindow.class, "保存失败" + ex.getMessage());
        }
    }

    /**
     * 覆盖保存默认漏洞库
     */
    private void onOverwriteYaml() {
        File file = new File(MiscUtils.getAbsolutePath(VULN_WIKI_FILE_PATH));
        if (!file.exists()) {
            showAlert(Alert.AlertType.ERROR, "错误", "默认漏洞库文件不存在：" + MiscUtils.getAbsolutePath(VULN_WIKI_FILE_PATH));
            return;
        }
        saveYaml(file);
    }

    /**
     * 另存为YAML文件
     * @param stage 窗口
     */
    private void onSaveYaml(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("另存为 YAML 文件");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("YAML 文件", "*.yaml", "*.yml"));
        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            saveYaml(file);
        }
    }

    /**
     * 加载YAML文件
     * @param stage 窗口
     */
    private void onLoadYaml(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("打开 YAML 文件");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("YAML 文件", "*.yaml", "*.yml"));
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            loadYaml(file);
        }
    }

    /**
     * 加载选中漏洞到表单
     */
    private void loadSelectedToForm() {
        Vulnerability selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        nameField.setText(selected.getName());
        harmArea.setText(selected.getHarm());
        riskField.setText(selected.getRisklevel());
        descArea.setText(selected.getDescription());
        suggestArea.setText(selected.getSuggustion());
    }

    /**
     * 新增漏洞
     */
    private void onNew() {
        Vulnerability v = new Vulnerability("新漏洞", "", "", "中危", "");
        masterData.add(v);
        table.getSelectionModel().select(v);
        loadSelectedToForm();
        nameField.requestFocus();
    }

    /**
     * 保存漏洞
     */
    private void onSave() {
        Vulnerability selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Vulnerability v = new Vulnerability(
                    nameField.getText(),
                    harmArea.getText(),
                    descArea.getText(),
                    riskField.getText(),
                    suggestArea.getText()
            );
            masterData.add(v);
            table.getSelectionModel().select(v);
        } else {
            selected.setName(nameField.getText());
            selected.setHarm(harmArea.getText());
            selected.setRisklevel(riskField.getText());
            selected.setDescription(descArea.getText());
            selected.setSuggustion(suggestArea.getText());
            table.refresh();
        }
    }

    /**
     * 删除漏洞
     */
    private void onDelete() {
        Vulnerability selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            masterData.remove(selected);
        }
    }

    /**
     * 显示提示框
     * @param type 类型
     * @param title 标题
     * @param msg 消息
     */
    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    /**
     * 漏洞模型
     */
    public static class Vulnerability {
        private final SimpleStringProperty name = new SimpleStringProperty("");
        private final SimpleStringProperty harm = new SimpleStringProperty("");
        private final SimpleStringProperty description = new SimpleStringProperty("");
        private final SimpleStringProperty risklevel = new SimpleStringProperty("");
        private final SimpleStringProperty suggustion = new SimpleStringProperty("");

        /**
         * 构造函数
         * @param name 名称
         * @param harm 危害
         * @param description 描述
         * @param risklevel 风险等级
         * @param suggustion 建议
         */
        public Vulnerability(String name, String harm, String description, String risklevel, String suggustion) {
            this.name.set(name);
            this.harm.set(harm);
            this.description.set(description);
            this.risklevel.set(risklevel);
            this.suggustion.set(suggustion);
        }

        public String getName() { return name.get(); }
        public void setName(String v) { name.set(v); }
        public String getHarm() { return harm.get(); }
        public void setHarm(String v) { harm.set(v); }
        public String getDescription() { return description.get(); }
        public void setDescription(String v) { description.set(v); }
        public String getRisklevel() { return risklevel.get(); }
        public void setRisklevel(String v) { risklevel.set(v); }
        public String getSuggustion() { return suggustion.get(); }
        public void setSuggustion(String v) { suggustion.set(v); }

        /**
         * 转换为Map
         * @return Map
         */
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", getName());
            m.put("harm", getHarm());
            m.put("description", getDescription());
            m.put("risklevel", getRisklevel());
            m.put("suggustion", getSuggustion());
            return m;
        }

        /**
         * 从Map创建漏洞
         * @param rawMap Map
         * @return 漏洞
         */
        @SuppressWarnings("unchecked")
        public static Vulnerability fromMap(Map<?, ?> rawMap) {
            Map<Object, Object> m = (Map<Object, Object>) rawMap;

            Object n = m.getOrDefault("name", "");
            Object h = m.getOrDefault("harm", "");
            Object d = m.getOrDefault("description", "");
            Object r = m.getOrDefault("risklevel", "");
            Object s = m.getOrDefault("suggustion", "");

            return new Vulnerability(
                    n == null ? "" : n.toString(),
                    h == null ? "" : h.toString(),
                    d == null ? "" : d.toString(),
                    r == null ? "" : r.toString(),
                    s == null ? "" : s.toString()
            );
        }
    }
}
