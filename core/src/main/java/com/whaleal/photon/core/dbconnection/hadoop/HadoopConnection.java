package com.whaleal.photon.core.dbconnection.hadoop;

import com.whaleal.photon.common.common.photonV.entity.Datasource;
import com.whaleal.photon.common.util.Log;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description:
 * @author: lhp
 * @time: 2021/9/22 4:30 下午
 */
public class HadoopConnection {
    /**
     * hadoop的链接
     */
    private static final Map<String, FileSystem> FILE_SYSTEM_MAP = new ConcurrentHashMap<>();

    /**
     * createHadoopFileSystem 创造hadoop客户端
     *
     * @param dsName
     * @desc 创造hadoop客户端。dcl检查
     */
    public static void createHadoopFileSystem(String dsName, Datasource datasource) {
        if (FILE_SYSTEM_MAP.containsKey(dsName)) {
            return;
        }
        try {
            Configuration conf = new Configuration();
            FileSystem fileSystem = FileSystem.get(new URI("hdfs://hadoop1:9000"), conf, "root");
            FILE_SYSTEM_MAP.put(dsName, fileSystem);
        } catch (Exception e) {
            Log.error("链接HDFS:" + datasource.getName() + "数据源出现异常,错误信息:" + e.getMessage());
        }

    }

    /**
     * getMHadoopFileSystem 获取HadoopFileSystem客户端
     *
     * @param dsName
     * @return FileSystem
     * @desc 获取HadoopFileSystem客户端
     */
    public static FileSystem getHadoopFileSystem(String dsName) {
        return FILE_SYSTEM_MAP.get(dsName);
    }

    /**
     * close 关闭FileSystem客户端
     *
     * @param dsName
     * @desc 关闭FileSystem客户端
     */
    public static void close(String dsName) {
        if (!FILE_SYSTEM_MAP.containsKey(dsName)) {
            return;
        }
        try {
            FILE_SYSTEM_MAP.get(dsName).close();
        } catch (Exception e) {
            Log.error("关闭HDFS客户端链接发生异常,错误信息:" + e.getMessage());
        } finally {
            FILE_SYSTEM_MAP.remove(dsName);
            Log.info("成功关闭HDFS链接:" + dsName);
        }
    }
}
