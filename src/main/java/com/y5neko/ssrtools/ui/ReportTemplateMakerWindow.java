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

import java.util.Optional;

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
import static com.y5neko.ssrtools.config.GlobalConfig.REPORT_TEMPLATE_DIR;

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
    private String lastGeneratedDocxPath; // 最后生成的docx文件路径

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

        Label uploadTitle = new Label("文件上传");
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

        fixButton = new Button("修复占位符");
        fixButton.setStyle(warningBtnStyle);
        fixButton.setOnMouseEntered(e -> fixButton.setStyle(warningBtnHover));
        fixButton.setOnMouseExited(e -> fixButton.setStyle(warningBtnStyle));
        fixButton.setDisable(true);

        exportButton = new Button("保存模板");
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

        Label logTitle = new Label("操作日志");
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
        exportButton.setOnAction(e -> saveTemplate());
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
                uploadButton.setText("选择Word模板文件 (.doc/.docx)");
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
                    uploadButton.setText("重新选择文件");
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
     * 打开文件
     */
    private void openFile(String filePath) {
        try {
            // macOS系统命令打开文件
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder processBuilder;

            if (os.contains("mac")) {
                processBuilder = new ProcessBuilder("open", filePath);
            } else if (os.contains("windows")) {
                processBuilder = new ProcessBuilder("cmd", "/c", "start", "\"\"", filePath, "\"\"");
            } else if (os.contains("linux")) {
                processBuilder = new ProcessBuilder("xdg-open", filePath);
            } else {
                // 默认使用Desktop类
                java.awt.Desktop.getDesktop().open(new File(filePath));
                return;
            }

            processBuilder.start();

        } catch (Exception e) {
            LogUtils.error(ReportTemplateMakerWindow.class, "打开文件失败", e);
            showAlert("错误", "无法打开文件：" + e.getMessage());
        }
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

                    // 生成测试docx文件
                    String testDocxPath = generateTestDocx();

                    Platform.runLater(() -> {
                        updateStatus("占位符修复完成！已生成测试文件", 1.0);
                        appendLog("占位符修复成功");
                        appendLog("修复统计：");
                        appendLog("- 原始完整占位符: " + WordPlaceholderTest.getOriginalPlaceholderCount());
                        appendLog("- 修复后完整占位符: " + WordPlaceholderTest.getFixedPlaceholderCount());
                        appendLog("已生成测试文件: " + testDocxPath);

                        // 弹出提示窗口
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("修复完成");
                        alert.setHeaderText("占位符修复成功！");
                        alert.setContentText("已自动生成测试文档供您查看格式。\n\n请检查格式是否有混乱，如存在混乱可能需要手动调整占位符。\n{{{{{MainContent}}}}}占位符消失是正常现象，可以忽略。\n\n点击确定后将自动打开测试文档。");

                        ButtonType result = alert.showAndWait().orElse(ButtonType.OK);

                        // 用户点击确定后自动打开文件
                        if (result == ButtonType.OK) {
                            openFile(testDocxPath);
                        }

                        // 保存最后生成的docx路径
                        lastGeneratedDocxPath = testDocxPath;

                        // 启用保存按钮
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
     * 保存模板
     */
    private void saveTemplate() {
        if (!isProcessed || templateDirPath == null) {
            showAlert("错误", "请先修复占位符");
            return;
        }

        // 检查模板目录是否存在，如果存在则询问用户
        File reportTemplateDir = new File(MiscUtils.getAbsolutePath(GlobalConfig.REPORT_TEMPLATE_DIR));
        if (!reportTemplateDir.exists()) {
            reportTemplateDir.mkdirs();
        }

        String saveDirPath = reportTemplateDir.getAbsolutePath() + File.separator + templateName;
        File saveDir = new File(saveDirPath);

        // 处理重复目录
        String finalDirPath = saveDirPath;
        boolean shouldOverride = false;

        if (saveDir.exists()) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("模板目录已存在");
                alert.setHeaderText("模板目录 '" + templateName + "' 已存在");
                alert.setContentText("请选择操作：");

                ButtonType overrideButton = new ButtonType("覆盖现有目录");
                ButtonType coexistButton = new ButtonType("创建新目录 (" + templateName + "_New)");
                ButtonType cancelButton = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);

                alert.getButtonTypes().setAll(overrideButton, coexistButton, cancelButton);

                Optional<ButtonType> result = alert.showAndWait();

                if (result.isPresent()) {
                    if (result.get() == overrideButton) {
                        // 用户选择覆盖，在后台线程中执行
                        executeSaveWithOverride(finalDirPath);
                    } else if (result.get() == coexistButton) {
                        // 用户选择共存，生成新的目录名
                        String newDirPath = finalDirPath + "_New";
                        executeSaveWithNewName(newDirPath);
                    }
                    // 取消则不做任何操作
                }
            });
        } else {
            // 目录不存在，直接保存
            executeSaveWithNewName(finalDirPath);
        }
    }

    /**
     * 覆盖现有目录保存模板
     */
    private void executeSaveWithOverride(String saveDirPath) {
        CompletableFuture.runAsync(() -> {
            try {
                File saveDir = new File(saveDirPath);

                Platform.runLater(() -> {
                    updateStatus("正在清理现有目录...", 0.5);
                    appendLog("清理现有模板目录: " + saveDirPath);
                });

                // 清理现有目录
                FileUtils.cleanDirectory(saveDirPath);

                // 继续保存流程
                continueSaveProcess(saveDirPath, false);

            } catch (Exception e) {
                Platform.runLater(() -> {
                    LogUtils.error(ReportTemplateMakerWindow.class, "清理现有目录失败", e);
                    showAlert("错误", "清理现有目录失败：" + e.getMessage());
                    updateStatus("保存失败", 0);
                    appendLog("错误：" + e.getMessage());
                });
            }
        });
    }

    /**
     * 使用新名称保存模板
     */
    private void executeSaveWithNewName(String saveDirPath) {
        CompletableFuture.runAsync(() -> {
            try {
                File saveDir = new File(saveDirPath);
                if (saveDir.exists()) {
                    Platform.runLater(() -> {
                        updateStatus("正在清理目录...", 0.5);
                        appendLog("清理模板目录: " + saveDirPath);
                    });
                    FileUtils.cleanDirectory(saveDirPath);
                } else {
                    Platform.runLater(() -> {
                        updateStatus("正在创建目录...", 0.5);
                        appendLog("创建新模板目录: " + saveDirPath);
                    });
                    saveDir.mkdirs();
                }

                // 继续保存流程
                continueSaveProcess(saveDirPath, true);

            } catch (Exception e) {
                Platform.runLater(() -> {
                    LogUtils.error(ReportTemplateMakerWindow.class, "创建目录失败", e);
                    showAlert("错误", "创建目录失败：" + e.getMessage());
                    updateStatus("保存失败", 0);
                    appendLog("错误：" + e.getMessage());
                });
            }
        });
    }

    /**
     * 继续保存流程
     */
    private void continueSaveProcess(String saveDirPath, boolean isNewName) {
        CompletableFuture.runAsync(() -> {
            try {
                Platform.runLater(() -> {
                    updateStatus("正在保存模板元文件...", 0.8);
                    appendLog("开始保存模板元文件到: " + saveDirPath);
                });

                // 复制所有元文件到目标目录
                File sourceDir = new File(templateDirPath);
                copyDirectory(sourceDir, new File(saveDirPath));

                Platform.runLater(() -> {
                    updateStatus("模板保存完成！", 1.0);
                    appendLog("模板元文件保存成功");
                    appendLog("保存位置: " + saveDirPath);

                    // 计算复制的文件数量和总大小
                    File[] copiedFiles = new File(saveDirPath).listFiles();
                    int fileCount = copiedFiles != null ? copiedFiles.length : 0;
                    long totalSize = calculateDirectorySize(new File(saveDirPath));
                    appendLog("复制文件数量: " + fileCount);
                    appendLog("总大小: " + totalSize + " 字节");

                    String finalTemplateName = isNewName ? templateName + "_New" : templateName;
                    showAlert("保存成功", "模板元文件已成功保存到：\n\n" + saveDirPath + "\n\n共 " + fileCount + " 个文件\n您现在可以在主界面中选择使用这个模板了。");

                    // 重置状态
                    isProcessed = false;
                    exportButton.setDisable(true);
                    fixButton.setDisable(true);
                    uploadButton.setText("选择Word模板文件 (.doc/.docx)");
                    updateStatus("等待上传文件...", 0);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    LogUtils.error(ReportTemplateMakerWindow.class, "保存模板失败", e);
                    showAlert("错误", "保存模板失败：" + e.getMessage());
                    updateStatus("保存失败", 0);
                    appendLog("错误：" + e.getMessage());
                });
            }
        });
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
     * 生成测试docx文件
     */
    private String generateTestDocx() {
        try {
            // 生成测试文件路径
            String testDocxPath = templateDirPath + "/" + templateName + "_test_" + MiscUtils.getRandomString(4) + ".docx";

            // 压缩文件为docx
            ZipUtils.zip(templateDirPath, testDocxPath);

            // 在事件线程中更新UI日志
            Platform.runLater(() -> appendLog("测试docx生成完成: " + testDocxPath));

            return testDocxPath;

        } catch (Exception e) {
            LogUtils.error(ReportTemplateMakerWindow.class, "生成测试docx失败", e);
            // 在事件线程中更新UI日志
            Platform.runLater(() -> appendLog("错误：生成测试docx失败 - " + e.getMessage()));
            return null;
        }
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

    /**
     * 复制整个目录（排除预览docx文件）
     */
    private void copyDirectory(File sourceDir, File targetDir) throws IOException {
        if (sourceDir.isDirectory()) {
            if (!targetDir.exists()) {
                targetDir.mkdir();
            }

            String[] files = sourceDir.list();
            if (files != null) {
                for (String file : files) {
                    // 跳过预览docx文件（包含"_test_"的docx文件）
                    if (file.toLowerCase().endsWith(".docx") && file.contains("_test_")) {
                        Platform.runLater(() -> appendLog("跳过预览文件: " + file));
                        continue;
                    }

                    File srcFile = new File(sourceDir, file);
                    File destFile = new File(targetDir, file);

                    if (srcFile.isDirectory()) {
                        // 递归复制子目录
                        copyDirectory(srcFile, destFile);
                    } else {
                        // 复制文件
                        java.nio.file.Files.copy(srcFile.toPath(), destFile.toPath(),
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

    /**
     * 计算目录总大小
     */
    private long calculateDirectorySize(File directory) {
        long size = 0;
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        size += calculateDirectorySize(file);
                    } else {
                        size += file.length();
                    }
                }
            }
        } else {
            size = directory.length();
        }
        return size;
    }
}