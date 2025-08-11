package com.y5neko.ssrtools.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LogUtils {

    // 获取对应类的 Logger
    public static Logger getLogger(Class<?> clazz) {
        return LogManager.getLogger(clazz);
    }

    // 方便直接打印 info
    public static void info(Class<?> clazz, String message) {
        getLogger(clazz).info(message);
    }

    public static void info(Class<?> clazz, String message, Throwable t) {
        getLogger(clazz).info(message, t);
    }

    public static void debug(Class<?> clazz, String message) {
        getLogger(clazz).debug(message);
    }

    public static void debug(Class<?> clazz, String message, Throwable t) {
        getLogger(clazz).debug(message, t);
    }

    public static void warn(Class<?> clazz, String message) {
        getLogger(clazz).warn(message);
    }

    public static void warn(Class<?> clazz, String message, Throwable t) {
        getLogger(clazz).warn(message, t);
    }

    public static void error(Class<?> clazz, String message) {
        getLogger(clazz).error(message);
    }

    public static void error(Class<?> clazz, String message, Throwable t) {
        getLogger(clazz).error(message, t);
    }

}
