package util;

import java.util.UUID;

/**
 * @description: 字符串工具类
 * @author: lhp
 * @time: 2021/8/30 1:56 下午
 */
public class StringUtil {
    public static String generateUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
}
