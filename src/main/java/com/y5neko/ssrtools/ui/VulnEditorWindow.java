package com.y5neko.ssrtools.ui;

import com.y5neko.ssrtools.utils.LogUtils;
import com.y5neko.ssrtools.utils.MiscUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
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
    private TextField harmField;
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
        stage.setScene(new Scene(root, 1000, 600));
        stage.show();
        loadDefaultVulnerabilities();
    }

    /**
     * 初始化控件
     */
    private void initControls() {
        filterField = new TextField();
        filterField.setPromptText("根据漏洞名称过滤...");

        table = new TableView<>();
        table.setPlaceholder(new Label("暂无漏洞"));

        nameField = new TextField();
        harmField = new TextField();
        riskField = new TextField();
        descArea = new TextArea();
        descArea.setPrefRowCount(6);
        descArea.setWrapText(true);
        suggestArea = new TextArea();
        suggestArea.setPrefRowCount(6);
        suggestArea.setWrapText(true);

        btnNew = new Button("新增漏洞");
        btnSave = new Button("保存到列表");
        btnDelete = new Button("删除选中");
        btnLoad = new Button("加载 YAML");
        btnOverwrite = new Button("保存 YAML");
        btnExport = new Button("另存为 YAML");
    }

    /**
     * 布局UI
     * @return 根节点
     */
    private BorderPane layoutUI() {
        // 表格列配置
        TableColumn<Vulnerability, String> nameCol = new TableColumn<>("漏洞名称");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(100);

        TableColumn<Vulnerability, String> riskCol = new TableColumn<>("风险等级");
        riskCol.setCellValueFactory(new PropertyValueFactory<>("risklevel"));
        riskCol.setPrefWidth(60);

        TableColumn<Vulnerability, String> harmCol = new TableColumn<>("漏洞危害");
        harmCol.setCellValueFactory(cell -> {
            String v = cell.getValue().getHarm();
            if (v == null) v = "";
            String preview = v.length() > 40 ? v.substring(0, 37) + "..." : v;
            return new SimpleStringProperty(preview);
        });
        harmCol.setPrefWidth(350);

        table.getColumns().setAll(nameCol, riskCol, harmCol);

        // 用过滤列表绑定表格
        table.setItems(filteredData);

        VBox tableBox = new VBox(5);
        tableBox.setPadding(new Insets(8));
        tableBox.getChildren().addAll(filterField, table);

        // 设置表格垂直扩展
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox form = new VBox(8);
        form.setPadding(new Insets(8));
        form.getChildren().addAll(
                new Label("漏洞名称"), nameField,
                new Label("漏洞危害"), harmField,
                new Label("风险等级"), riskField,
                new Label("漏洞描述"), descArea,
                new Label("建议/修复"), suggestArea
        );
        HBox buttons = new HBox(8, btnNew, btnSave, btnDelete, btnLoad, btnOverwrite, btnExport);
        form.getChildren().add(buttons);
        VBox.setVgrow(descArea, Priority.ALWAYS);
        VBox.setVgrow(suggestArea, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setCenter(tableBox);
        root.setRight(form);
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
        harmField.setText(selected.getHarm());
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
                    harmField.getText(),
                    descArea.getText(),
                    riskField.getText(),
                    suggestArea.getText()
            );
            masterData.add(v);
            table.getSelectionModel().select(v);
        } else {
            selected.setName(nameField.getText());
            selected.setHarm(harmField.getText());
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
