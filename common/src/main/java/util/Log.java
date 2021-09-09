package util;

import org.apache.log4j.Logger;
import org.bson.BsonTimestamp;

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
    private static final Logger Log = Logger.getLogger(Log.class.getName());

    public static void info(String message) {
        StackTraceElement callInfo = new Throwable().getStackTrace()[1];
        Log.info(logDetail(callInfo, message));
    }

    public static void error(String message) {
        StackTraceElement callInfo = new Throwable().getStackTrace()[1];
        Log.error(logDetail(callInfo, message));
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


    public static void main(String[] args) {
        // , 2862945894
        BsonTimestamp bsonTimestamp=new BsonTimestamp(2862945894L);


    }
}


