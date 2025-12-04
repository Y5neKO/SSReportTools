package com.y5neko.ssrtools;

import com.y5neko.ssrtools.config.CopyRight;
import com.y5neko.ssrtools.config.GlobalConfig;
import com.y5neko.ssrtools.utils.MiscUtils;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.y5neko.ssrtools.ui.MainWindow;
import org.scenicview.ScenicView;

public class GUI extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        stage.setTitle("安服/渗透测试 报告生成工具 by Y5neKO");

        // 初始化程序所需目录
        MiscUtils.initDir(GlobalConfig.REQUIRED_DIRS);

        // 显示版权信息
        CopyRight.showCopyright();

        // 显示主界面
        MainWindow mainView = new MainWindow();
        Scene scene = new Scene(mainView.getView(), 900, 500);

        stage.setScene(scene);
        stage.setMinWidth(850);
        stage.setMinHeight(480);
        stage.show();

        // 初始化 ScenicView
//        ScenicView.show(scene);
    }

    // 方便其他类切换场景
    public static void setRoot(Scene scene) {
        primaryStage.setScene(scene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
