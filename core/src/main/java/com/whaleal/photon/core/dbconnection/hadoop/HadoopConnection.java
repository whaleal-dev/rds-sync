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
    private static Map<String, FileSystem> fileSystemMap = new ConcurrentHashMap<>();

    /**
     * createHadoopFileSystem 创造hadoop客户端
     *
     * @param procNameAndBatchNoAndDsName
     * @desc 创造hadoop客户端。dcl检查
     */
    public static void createHadoopFileSystem(String procNameAndBatchNoAndDsName, Datasource datasource) {
        if (fileSystemMap.containsKey(procNameAndBatchNoAndDsName)) {
            return;
        }
        try {
            //FileSystem fileSystem = FileSystem.get(URI.create("1"), new Configuration());
            Configuration conf = new Configuration();
            FileSystem fileSystem = FileSystem.get(new URI("hdfs://hadoop1:9000"), conf, "root");
            fileSystemMap.put(procNameAndBatchNoAndDsName, fileSystem);
        } catch (Exception e) {
            Log.error(e.getMessage());
        }

    }

    /**
     * getMHadoopFileSystem 获取HadoopFileSystem客户端
     *
     * @param procNameAndBatchNoAndDsName
     * @return FileSystem
     * @desc 获取HadoopFileSystem客户端
     */
    public static FileSystem getHadoopFileSystem(String procNameAndBatchNoAndDsName) {
        return fileSystemMap.get(procNameAndBatchNoAndDsName);
    }

    /**
     * close 关闭FileSystem客户端
     *
     * @param procNameAndBatchNoAndDsName
     * @desc 关闭FileSystem客户端
     */
    public static void close(String procNameAndBatchNoAndDsName) {
        if (!fileSystemMap.containsKey(procNameAndBatchNoAndDsName)) {
            return;
        }
        try {
            fileSystemMap.get(procNameAndBatchNoAndDsName).close();
        } catch (Exception e) {
            Log.error(e.getMessage());
        } finally {
            fileSystemMap.remove(procNameAndBatchNoAndDsName);
            Log.info(procNameAndBatchNoAndDsName + ",fileSystem链接已关闭");
        }
    }
}
