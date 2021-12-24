package com.whaleal.photon.common.common.status;



import com.whaleal.photon.common.util.Log;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description: 程序状态符
 * @author: lhp
 * @time: 2021/12/1 10:09 上午
 */
public class ProStatus {

    public final static int PRO_STOP = -1;
    public final static int PRO_SLEEP = 0;
    public final static int PRO_RUN = 1;

    public final static int FULL_SYNC_RUN = 2;
    public final static int FULL_SYNC_SLEEP = 3;
    public final static int FULL_SYNC_STOP = 4;

    public final static int REAL_TIME_RUN = 5;
    public final static int REAL_TIME_SLEEP = 6;
    public final static int REAL_TIME_STOP = 7;

    public final static int FULL_SYNC_SLEEP_AND_REAL_TIME_RUN = 8;
    public final static int FULL_SYNC_RUN_AND_REAL_TIME_SLEEP = 9;
    public final static int FULL_SYNC_CLOSE_AND_REAL_TIME_RUN = 8;
    /**
     * 程序状态符Map
     * k为程序名
     * v为状态
     */
    private final static Map<String, Integer> PRO_STATUS_MAP = new ConcurrentHashMap<>();

    /**
     * 程序实时对象锁Map
     * k为程序名
     * v为实时对象锁
     */
    private final static Map<String, Object> PRO_REAL_TIME_OBJECT_LOCK = new ConcurrentHashMap<>();
    /**
     * 程序全量对象锁Map
     * k为程序名
     * v为全量对象锁
     */
    private final static Map<String, Object> PRO_FULL_SYNC_OBJECT_LOCK = new ConcurrentHashMap<>();


    public static Object getProRealTimeObjectLock(String proName) {
        return PRO_REAL_TIME_OBJECT_LOCK.getOrDefault(proName, null);
    }

    public static void updateProRealTimeObjectLock(String proName, Object object) {
        PRO_REAL_TIME_OBJECT_LOCK.put(proName, object);
    }

    public static void addProRealTimeObjectLock(String proName, Object object) {
        PRO_REAL_TIME_OBJECT_LOCK.put(proName, object);
    }


    public static Object getProFullSyncObjectLock(String proName) {
        return PRO_FULL_SYNC_OBJECT_LOCK.getOrDefault(proName, null);
    }

    public static void updateProFullSyncObjectLock(String proName, Object object) {
        PRO_FULL_SYNC_OBJECT_LOCK.put(proName, object);
    }

    public static void addProFullSyncObjectLock(String proName, Object object) {
        PRO_FULL_SYNC_OBJECT_LOCK.put(proName, object);
    }


    public static Integer getProStatus(String proName) {
        return PRO_STATUS_MAP.getOrDefault(proName, -1);
    }

    public static void updateProStatus(String proName, Integer status) {
        Log.warn("程序:" + proName + ",更新程序的状态为:" + status);
        PRO_STATUS_MAP.put(proName, status);
    }

    public static void addProStatus(String proName, Integer status) {
        PRO_STATUS_MAP.put(proName, status);

    }


    public static void main(String[] args) {

        System.out.println(getProStatus("1"));
        updateProStatus("1", 100);
        System.out.println(getProStatus("1"));
        final Object object = new Date();
        addProFullSyncObjectLock("1", object);
        System.out.println(getProFullSyncObjectLock("1"));
        Object object2 = getProFullSyncObjectLock("1");
        object2 = new Object();
        System.out.println(object2);
    }

}
