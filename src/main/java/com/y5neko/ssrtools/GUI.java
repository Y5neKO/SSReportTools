package com.y5neko.ssrtools;

import com.y5neko.ssrtools.config.CopyRight;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.y5neko.ssrtools.ui.MainWindow;

public class GUI extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        stage.setTitle("安服/渗透测试 报告生成工具 by Y5neKO");

        // 显示版权信息
        CopyRight.showCopyright();

        // 显示主界面
        MainWindow mainView = new MainWindow();
        Scene scene = new Scene(mainView.getView(), 800, 500);

        stage.setScene(scene);
        stage.show();
    }

    // 方便其他类切换场景
    public static void setRoot(Scene scene) {
        primaryStage.setScene(scene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
