package com.whaleal.photon.common.util;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;

/**
 * 日志记录类
 *
 * @author lhp
 * @time 2021-05-31 13:12:12
 */
public class Log {
    /**
     * Logger对象
     */

    private static final Logger log4j = LogManager.getLogger("HelloWorld");

    public static void info(String message) {
        log4j.info(message);
    }

    public static void warn(String message) {
        StackTraceElement callInfo = new Throwable().getStackTrace()[1];
        log4j.warn(logDetail(callInfo, message));
    }

    public static void error(String message) {
        StackTraceElement callInfo = new Throwable().getStackTrace()[1];
        log4j.error(logDetail(callInfo, message));
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (Exception ignored) {
            // 由于程序中较多的while循环,当出现异常时 可能无限循环打印错误日志。因此进行睡眠1s
        }
    }

    /**
     * logDetail 日志详情
     *
     * @desc 获取该条日志信息 在什么类中哪一行的位置
     */
    private static String logDetail(StackTraceElement call, String appendLog) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(" ")
                .append("[" + call.getFileName().replace("java", "") + call.getMethodName())
                .append("-" + call.getLineNumber() + "]").append(" ")
                .append(appendLog != null ? appendLog : "");
        return buffer.toString();
    }

}

