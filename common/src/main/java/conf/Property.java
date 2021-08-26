package conf;

import util.Log;

import java.io.FileInputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

/**
 * 配置文件类
 *
 * @author lhp
 * @time 2021-05-31 13:12:12
 */
public class Property {
    /**
     * 配置文件路径
     */
    private static String fileName = "src/main/resources/mongodbT.properties";
    /**
     * 配置信息K-V
     */
    private static HashMap<String, String> propertiesMap = new HashMap<String, String>();

    static {
        // 读取src下的配置文件
        setFileName("../mongodbT.properties");
        setFileName("mongodbT.properties");
        // 读取./下的配置文件
        setFileName("common/src/main/resources/mongodbT.properties");
    }

    public static void setFileName(String fileNameTemp) {
        fileName = fileNameTemp;
        readProperties();
    }

    /**
     * readProperties 读取配置文件
     *
     * @desc 读取配置文件
     */
    private static void readProperties() {
        try {
            Properties pps = new Properties();
            pps.load(new FileInputStream(fileName));
            Enumeration enum1 = pps.propertyNames();
            while (enum1.hasMoreElements()) {
                String strKey = (String) enum1.nextElement().toString().trim();
                String strValue = pps.getProperty(strKey).trim();
                propertiesMap.put(strKey, strValue);
            }
            Log.info(propertiesMap.toString());
        } catch (Exception e) {
            Log.error("配置文件读取失败:fileName:" + fileName + ",exception:" + e.getMessage());
        }
    }

    public static String getPropertiesByKey(String key) {
        //默认返回值为空字符串
        String value = "";
        if (propertiesMap.containsKey(key)) {
            value = propertiesMap.get(key);
        }
        return value.trim();
    }

}
