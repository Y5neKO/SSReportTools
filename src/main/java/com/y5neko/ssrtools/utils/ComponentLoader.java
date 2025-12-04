package com.y5neko.ssrtools.utils;

import com.y5neko.ssrtools.config.GlobalConfig;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 标题组件加载工具
 * 根据模板名称动态加载对应的标题组件文件
 */
public class ComponentLoader {

    // 组件文件映射
    private static final String[] COMPONENT_FILES = {
        "first_level_heading.txt",
        "second_level_heading.txt",
        "third_level_heading.txt",
        "fourth_level_heading.txt",
        "normal_text.txt"
    };

    /**
     * 加载指定模板的标题组件内容
     *
     * @param templateName 模板名称
     * @param componentFile 组件文件名
     * @return 组件内容
     * @throws IOException 文件读取异常
     */
    public static String loadComponent(String templateName, String componentFile) throws IOException {
        // 首先尝试从用户组件目录加载
        String userComponentPath = GlobalConfig.USER_COMPONENTS_DIR + File.separator + templateName + File.separator + componentFile;
        File userComponentFile = new File(MiscUtils.getAbsolutePath(userComponentPath));

        if (userComponentFile.exists()) {
            return FileUtils.readFile(MiscUtils.getAbsolutePath(userComponentPath));
        }

        // 如果用户组件不存在，使用默认组件
        String defaultComponentPath = GlobalConfig.DEFAULT_COMPONENTS_DIR + File.separator + componentFile;
        File defaultComponentFile = new File(MiscUtils.getAbsolutePath(defaultComponentPath));

        if (defaultComponentFile.exists()) {
            return FileUtils.readFile(MiscUtils.getAbsolutePath(defaultComponentPath));
        }

        // 如果默认组件也不存在，抛出异常
        throw new IOException("组件文件不存在: " + componentFile + " (用户和默认目录都没有找到)");
    }

    /**
     * 检查指定模板是否有对应的用户组件
     *
     * @param templateName 模板名称
     * @return 是否存在用户组件
     */
    public static boolean hasUserComponents(String templateName) {
        String userComponentDir = GlobalConfig.USER_COMPONENTS_DIR + File.separator + templateName;
        File userDir = new File(MiscUtils.getAbsolutePath(userComponentDir));

        if (!userDir.exists()) {
            return false;
        }

        // 检查是否包含所有必需的组件文件
        for (String componentFile : COMPONENT_FILES) {
            File componentFileObj = new File(userDir, componentFile);
            if (!componentFileObj.exists()) {
                return false;
            }
        }

        return true;
    }

    /**
     * 获取指定模板的所有组件文件路径
     *
     * @param templateName 模板名称
     * @return 组件文件路径数组
     */
    public static String[] getComponentPaths(String templateName) {
        String[] paths = new String[COMPONENT_FILES.length];
        String baseDir;

        // 优先使用用户组件目录
        if (hasUserComponents(templateName)) {
            baseDir = GlobalConfig.USER_COMPONENTS_DIR + File.separator + templateName;
        } else {
            baseDir = GlobalConfig.DEFAULT_COMPONENTS_DIR;
        }

        for (int i = 0; i < COMPONENT_FILES.length; i++) {
            paths[i] = baseDir + File.separator + COMPONENT_FILES[i];
        }

        return paths;
    }

    /**
     * 复制默认组件到用户组件目录
     *
     * @param templateName 模板名称
     * @throws IOException 文件操作异常
     */
    public static void copyDefaultComponentsToUser(String templateName) throws IOException {
        String targetDir = GlobalConfig.USER_COMPONENTS_DIR + File.separator + templateName;
        File targetDirFile = new File(MiscUtils.getAbsolutePath(targetDir));

        // 创建目标目录
        if (!targetDirFile.exists()) {
            targetDirFile.mkdirs();
        }

        // 复制所有组件文件
        for (String componentFile : COMPONENT_FILES) {
            String sourcePath = GlobalConfig.DEFAULT_COMPONENTS_DIR + File.separator + componentFile;
            String targetPath = targetDir + File.separator + componentFile;

            File sourceFile = new File(MiscUtils.getAbsolutePath(sourcePath));
            File targetFile = new File(MiscUtils.getAbsolutePath(targetPath));

            if (sourceFile.exists()) {
                Files.copy(sourceFile.toPath(), targetFile.toPath());
            }
        }
    }

    /**
     * 获取组件文件描述
     *
     * @param componentFile 组件文件名
     * @return 组件描述
     */
    public static String getComponentDescription(String componentFile) {
        switch (componentFile) {
            case "first_level_heading.txt":
                return "一级标题组件";
            case "second_level_heading.txt":
                return "二级标题组件";
            case "third_level_heading.txt":
                return "三级标题组件";
            case "fourth_level_heading.txt":
                return "四级标题组件";
            case "normal_text.txt":
                return "正文文本组件";
            default:
                return "未知组件";
        }
    }
}