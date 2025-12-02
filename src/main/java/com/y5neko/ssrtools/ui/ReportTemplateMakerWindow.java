package com.y5neko.ssrtools.ui;

import com.y5neko.ssrtools.config.GlobalConfig;
import com.y5neko.ssrtools.utils.FileUtils;
import com.y5neko.ssrtools.utils.LogUtils;
import com.y5neko.ssrtools.utils.MiscUtils;
import com.y5neko.ssrtools.utils.ZipUtils;
import com.y5neko.ssrtools.utils.WordPlaceholderTest;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.y5neko.ssrtools.config.GlobalConfig.TEMPLATE_MAKER_CACHE_DIR;

/**
 * 报告模板制作窗口
 * 用于上传、修复和导出Word模板文件
 */
public class ReportTemplateMakerWindow {

    private Stage stage;
    private VBox mainContainer;

    // 界面组件
    private Label statusLabel;
    private ProgressBar progressBar;
    private Button uploadButton;
    private Button fixButton;
    private Button exportButton;
    private TextArea logArea;

    // 文件状态
    private File uploadedFile;
    private String templateDirPath;
    private String templateName;
    private boolean isProcessed = false;

    // 按钮样式
    private String primaryBtnStyle = "-fx-background-color: #4361ee; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 4px; -fx-padding: 8px 16px; -fx-font-size: 12px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(67, 97, 238, 0.2), 3, 0, 0, 1);";
    private String primaryBtnHover = "-fx-background-color: #3651de; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 4px; -fx-padding: 8px 16px; -fx-font-size: 12px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(67, 97, 238, 0.4), 4, 0, 0, 1);";

    private String successBtnStyle = "-fx-background-color: #26de81; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 4px; -fx-padding: 8px 16px; -fx-font-size: 12px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(38, 222, 129, 0.2), 3, 0, 0, 1);";
    private String successBtnHover = "-fx-background-color: #1eb980; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 4px; -fx-padding: 8px 16px; -fx-font-size: 12px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(38, 222, 129, 0.4), 4, 0, 0, 1);";

    private String warningBtnStyle = "-fx-background-color: #fd9644; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 4px; -fx-padding: 8px 16px; -fx-font-size: 12px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(253, 150, 68, 0.2), 3, 0, 0, 1);";
    private String warningBtnHover = "-fx-background-color: #fa8231; -fx-text-fill: white; -fx-font-weight: 600; -fx-border-radius: 4px; -fx-padding: 8px 16px; -fx-font-size: 12px; -fx-cursor: hand; -fx-border-width: 1px; -fx-border-color: transparent; -fx-background-insets: 0; -fx-effect: dropshadow(gaussian, rgba(253, 150, 68, 0.4), 4, 0, 0, 1);";

    public ReportTemplateMakerWindow() {
        // 启动时清理缓存目录
        clearCacheDirectory();
        setupUI();
        setupEventHandlers();
    }

    /**
     * 设置UI界面
     */
    private void setupUI() {
        mainContainer = new VBox(15);
        mainContainer.setPadding(new Insets(20));
        mainContainer.setStyle("-fx-background-color: #f8f9fa;");

        // 标题
        Label titleLabel = new Label("报告模板制作工具");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #2d3436;");
        mainContainer.getChildren().add(titleLabel);

        // 说明文本
        Label descLabel = new Label("上传Word模板文件，自动修复占位符，然后导出可用的模板文件");
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #636e72;");
        mainContainer.getChildren().add(descLabel);

        // 分隔线
        Separator separator = new Separator();
        mainContainer.getChildren().add(separator);

        // 文件上传区域
        VBox uploadBox = createUploadArea();
        mainContainer.getChildren().add(uploadBox);

        // 操作按钮区域
        HBox buttonBox = createButtonArea();
        mainContainer.getChildren().add(buttonBox);

        // 进度条和状态
        VBox statusBox = createStatusArea();
        mainContainer.getChildren().add(statusBox);

        // 日志区域
        VBox logBox = createLogArea();
        mainContainer.getChildren().add(logBox);

        VBox.setVgrow(logBox, Priority.ALWAYS);
    }

    /**
     * 创建文件上传区域
     */
    private VBox createUploadArea() {
        VBox uploadBox = new VBox(10);
        uploadBox.setPadding(new Insets(15));
        uploadBox.setStyle("-fx-background-color: white; -fx-border-radius: 8px; -fx-border-color: #dfe6e9; -fx-border-width: 1px;");

        Label uploadTitle = new Label("📁 文件上传");
        uploadTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2d3436;");
        uploadBox.getChildren().add(uploadTitle);

        uploadButton = new Button("选择Word模板文件 (.doc/.docx)");
        uploadButton.setStyle(primaryBtnStyle);
        uploadButton.setOnMouseEntered(e -> uploadButton.setStyle(primaryBtnHover));
        uploadButton.setOnMouseExited(e -> uploadButton.setStyle(primaryBtnStyle));
        uploadBox.getChildren().add(uploadButton);

        return uploadBox;
    }

    /**
     * 创建操作按钮区域
     */
    private HBox createButtonArea() {
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10, 0, 10, 0));

        fixButton = new Button("🔧 修复占位符");
        fixButton.setStyle(warningBtnStyle);
        fixButton.setOnMouseEntered(e -> fixButton.setStyle(warningBtnHover));
        fixButton.setOnMouseExited(e -> fixButton.setStyle(warningBtnStyle));
        fixButton.setDisable(true);

        exportButton = new Button("💾 导出模板");
        exportButton.setStyle(successBtnStyle);
        exportButton.setOnMouseEntered(e -> exportButton.setStyle(successBtnHover));
        exportButton.setOnMouseExited(e -> exportButton.setStyle(successBtnStyle));
        exportButton.setDisable(true);

        buttonBox.getChildren().addAll(fixButton, exportButton);
        return buttonBox;
    }

    /**
     * 创建状态区域
     */
    private VBox createStatusArea() {
        VBox statusBox = new VBox(5);
        statusBox.setPadding(new Insets(10));
        statusBox.setStyle("-fx-background-color: white; -fx-border-radius: 8px; -fx-border-color: #dfe6e9; -fx-border-width: 1px;");

        statusLabel = new Label("等待上传文件...");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #636e72;");
        statusBox.getChildren().add(statusLabel);

        progressBar = new ProgressBar();
        progressBar.setProgress(0);
        progressBar.setVisible(false);
        statusBox.getChildren().add(progressBar);

        return statusBox;
    }

    /**
     * 创建日志区域
     */
    private VBox createLogArea() {
        VBox logBox = new VBox(5);
        logBox.setPadding(new Insets(10));
        logBox.setStyle("-fx-background-color: white; -fx-border-radius: 8px; -fx-border-color: #dfe6e9; -fx-border-width: 1px;");
        logBox.setMinHeight(200);

        Label logTitle = new Label("📝 操作日志");
        logTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2d3436;");
        logBox.getChildren().add(logTitle);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 11px; -fx-background-color: #f8f9fa; -fx-border-radius: 4px; -fx-border-color: #dfe6e9; -fx-border-width: 1px;");
        logArea.setPrefRowCount(10);
        logBox.getChildren().add(logArea);

        return logBox;
    }

    /**
     * 清理缓存目录
     */
    private void clearCacheDirectory() {
        try {
            String cachePath = MiscUtils.getAbsolutePath(TEMPLATE_MAKER_CACHE_DIR);
            File cacheDir = new File(cachePath);
            if (cacheDir.exists()) {
                FileUtils.cleanDirectory(cachePath);
                LogUtils.info(ReportTemplateMakerWindow.class, "已清理模板制作缓存目录: " + cachePath);
            }
        } catch (Exception e) {
            LogUtils.error(ReportTemplateMakerWindow.class, "清理缓存目录失败", e);
        }
    }

    /**
     * 设置事件处理器
     */
    private void setupEventHandlers() {
        uploadButton.setOnAction(e -> uploadFile());
        fixButton.setOnAction(e -> fixPlaceholders());
        exportButton.setOnAction(e -> exportTemplate());
    }

    /**
     * 上传文件
     */
    private void uploadFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择Word模板文件");

        // 设置文件过滤器
        FileChooser.ExtensionFilter docFilter = new FileChooser.ExtensionFilter("Word文档 (*.doc, *.docx)", "*.doc", "*.docx");
        FileChooser.ExtensionFilter allFilter = new FileChooser.ExtensionFilter("所有文件", "*.*");
        fileChooser.getExtensionFilters().addAll(docFilter, allFilter);

        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            // 如果之前有处理的文件，先清理缓存
            if (isProcessed) {
                clearCacheDirectory();
                isProcessed = false;
                fixButton.setDisable(true);
                exportButton.setDisable(true);
                uploadButton.setText("📁 选择Word模板文件 (.doc/.docx)");
                updateStatus("等待上传文件...", 0);
                logArea.clear();
            }
            uploadedFile = selectedFile;
            processUploadedFile();
        }
    }

    /**
     * 处理上传的文件
     */
    private void processUploadedFile() {
        updateStatus("正在处理上传的文件...", 0.1);
        appendLog("开始处理文件: " + uploadedFile.getName());

        CompletableFuture.runAsync(() -> {
            try {
                // 获取文件名（不带扩展名）
                String fileName = uploadedFile.getName();
                int dotIndex = fileName.lastIndexOf('.');
                templateName = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;

                // 创建缓存目录
                File cacheDir = new File(MiscUtils.getAbsolutePath(TEMPLATE_MAKER_CACHE_DIR));
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs();
                }

                final String finalTemplateDirPath = cacheDir.getAbsolutePath() + File.separator + templateName;
                File newTemplateDir = new File(finalTemplateDirPath);
                final boolean isCleaned = newTemplateDir.exists();
                if (isCleaned) {
                    FileUtils.cleanDirectory(finalTemplateDirPath);
                } else {
                    newTemplateDir.mkdirs();
                }

                Platform.runLater(() -> {
                    updateStatus("正在解压文件...", 0.3);
                    appendLog("开始解压文件到缓存目录");
                    if (isCleaned) {
                        appendLog("清理现有缓存目录: " + finalTemplateDirPath);
                    } else {
                        appendLog("创建缓存目录: " + finalTemplateDirPath);
                    }
                });

                // 设置类变量
                templateDirPath = finalTemplateDirPath;

                // 解压文件到缓存目录
                if (uploadedFile.getName().toLowerCase().endsWith(".docx")) {
                    ZipUtils.extractZip(uploadedFile.getAbsolutePath(), templateDirPath);
                } else {
                    // 对于.doc文件，提示用户
                    Platform.runLater(() -> {
                        showAlert("提示", "请将.doc文件转换为.docx格式后再上传");
                        updateStatus("等待上传文件...", 0);
                        return;
                    });
                    return;
                }

                Platform.runLater(() -> {
                    updateStatus("检查解压结果...", 0.6);
                    appendLog("文件解压完成");
                });

                // 验证解压结果
                File documentXml = new File(templateDirPath + "/word/document.xml");
                if (!documentXml.exists()) {
                    Platform.runLater(() -> {
                        showAlert("错误", "文件格式不正确，无法找到document.xml文件");
                        updateStatus("解压失败", 0);
                        appendLog("错误：找不到document.xml文件");
                    });
                    return;
                }

                Platform.runLater(() -> {
                    updateStatus("文件上传成功！可以开始修复占位符", 1.0);
                    appendLog("文件验证成功，准备修复占位符");
                    appendLog("找到document.xml文件，大小: " + documentXml.length() + " 字节");

                    // 启用修复按钮
                    fixButton.setDisable(false);
                    uploadButton.setText("📁 重新选择文件");
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    LogUtils.error(ReportTemplateMakerWindow.class, "处理上传文件失败", e);
                    showAlert("错误", "处理文件失败：" + e.getMessage());
                    updateStatus("处理失败", 0);
                    appendLog("错误：" + e.getMessage());
                });
            }
        });
    }

    /**
     * 修复占位符
     */
    private void fixPlaceholders() {
        if (templateDirPath == null) {
            showAlert("错误", "请先上传文件");
            return;
        }

        updateStatus("正在修复占位符...", 0.5);
        appendLog("开始修复占位符...");

        CompletableFuture.runAsync(() -> {
            try {
                String documentXmlPath = templateDirPath + "/word/document.xml";
                String oldDocumentXmlPath = templateDirPath + "/word/old_document.xml";

                // 备份原始文件（如果存在则覆盖）
                Files.copy(Paths.get(documentXmlPath), Paths.get(oldDocumentXmlPath),
                         java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                Platform.runLater(() -> {
                    appendLog("已备份原始文件为: old_document.xml");
                });

                // 读取文件内容
                String originalContent = FileUtils.readFile(documentXmlPath);

                // 调用修复逻辑
                String fixedContent = WordPlaceholderTest.fixPlaceholders(originalContent);

                if (fixedContent != null) {
                    // 写入修复后的内容
                    FileUtils.overwrite(documentXmlPath, fixedContent, StandardCharsets.UTF_8);

                    Platform.runLater(() -> {
                        updateStatus("占位符修复完成！可以导出模板", 1.0);
                        appendLog("占位符修复成功");
                        appendLog("修复统计：");
                        appendLog("- 原始完整占位符: " + WordPlaceholderTest.getOriginalPlaceholderCount());
                        appendLog("- 修复后完整占位符: " + WordPlaceholderTest.getFixedPlaceholderCount());

                        // 启用导出按钮
                        exportButton.setDisable(false);
                        fixButton.setDisable(true);
                        isProcessed = true;
                    });
                } else {
                    Platform.runLater(() -> {
                        updateStatus("占位符修复失败", 0);
                        appendLog("占位符修复失败");
                        showAlert("错误", "占位符修复失败");
                    });
                }

            } catch (Exception e) {
                Platform.runLater(() -> {
                    LogUtils.error(ReportTemplateMakerWindow.class, "修复占位符失败", e);
                    showAlert("错误", "修复占位符失败：" + e.getMessage());
                    updateStatus("修复失败", 0);
                    appendLog("错误：" + e.getMessage());
                });
            }
        });
    }

    /**
     * 导出模板
     */
    private void exportTemplate() {
        if (!isProcessed || templateDirPath == null) {
            showAlert("错误", "请先修复占位符");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("保存修复后的模板");
        fileChooser.setInitialFileName(templateName + "_fixed.docx");

        FileChooser.ExtensionFilter docxFilter = new FileChooser.ExtensionFilter("Word文档 (*.docx)", "*.docx");
        fileChooser.getExtensionFilters().add(docxFilter);

        File saveFile = fileChooser.showSaveDialog(stage);
        if (saveFile != null) {
            exportTemplateToFile(saveFile);
        }
    }

    /**
     * 导出模板到文件
     */
    private void exportTemplateToFile(File targetFile) {
        updateStatus("正在导出模板...", 0.8);
        appendLog("开始导出模板到: " + targetFile.getAbsolutePath());

        CompletableFuture.runAsync(() -> {
            try {
                // 收集需要压缩的文件
                List<File> filesToCompress = new ArrayList<>();
                File templateDir = new File(templateDirPath);

                collectFiles(templateDir, filesToCompress);

                // 压缩文件 - 使用目录压缩方式
                ZipUtils.zip(templateDirPath, targetFile.getAbsolutePath());

                Platform.runLater(() -> {
                    updateStatus("模板导出完成！", 1.0);
                    appendLog("模板导出成功");
                    appendLog("导出文件大小: " + targetFile.length() + " 字节");

                    showAlert("导出完成", "模板导出成功！\n\n请打开检查一下格式有没有混乱。\n如存在混乱可能需要手动调整一下占位符。\n{{{{{MainContent}}}}}占位符消失是正常现象，可以忽略。");

                    // 重置状态
                    isProcessed = false;
                    exportButton.setDisable(true);
                    fixButton.setDisable(true);
                    uploadButton.setText("📁 选择Word模板文件 (.doc/.docx)");
                    updateStatus("等待上传文件...", 0);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    LogUtils.error(ReportTemplateMakerWindow.class, "导出模板失败", e);
                    showAlert("错误", "导出模板失败：" + e.getMessage());
                    updateStatus("导出失败", 0);
                    appendLog("错误：" + e.getMessage());
                });
            }
        });
    }

    /**
     * 递归收集文件
     */
    private void collectFiles(File dir, List<File> fileList) throws IOException {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    collectFiles(file, fileList);
                } else {
                    // 计算相对路径
                    String relativePath = file.getAbsolutePath().substring(templateDirPath.length());
                    fileList.add(file);
                }
            }
        }
    }

    /**
     * 更新状态
     */
    private void updateStatus(String message, double progress) {
        statusLabel.setText(message);
        progressBar.setProgress(progress);
        progressBar.setVisible(progress > 0 && progress < 1.0);
    }

    /**
     * 添加日志
     */
    private void appendLog(String message) {
        String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        logArea.appendText("[" + timestamp + "] " + message + "\n");
        // 自动滚动到底部
        logArea.setScrollTop(Double.MAX_VALUE);
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
        return mainContainer;
    }

    /**
     * 显示窗口
     */
    public void show() {
        stage = new Stage();
        stage.setTitle("报告模板制作工具");
        stage.setScene(new Scene(getView(), 600, 700));
        stage.setResizable(true);
        stage.setMinWidth(550);
        stage.setMinHeight(600);

        // 添加窗口关闭事件处理
        stage.setOnCloseRequest(event -> {
            clearCacheDirectory();
            stage.close();
        });

        stage.showAndWait();
    }
}