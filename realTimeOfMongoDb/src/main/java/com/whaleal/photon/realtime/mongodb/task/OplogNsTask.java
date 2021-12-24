package com.whaleal.photon.realtime.mongodb.task;


import com.whaleal.photon.common.common.status.ProStatus;
import com.whaleal.photon.common.util.Log;
import com.whaleal.photon.core.thread.RealTimeTaskPoolManager;
import com.whaleal.photon.realtime.mongodb.common.OplogMetadata;
import org.bson.Document;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * @author: lhp
 * @time: 2021/7/21 2:38 下午
 * @desc: 解析document的ns
 */
public class OplogNsTask implements Runnable {
    /**
     * oplog元数据库类
     */
    private final OplogMetadata oplogMetadata;
    /**
     * 表过滤策略 正则表达式处理,着重处理ddl操作表
     */
    private final String dbTableWhite;
    /**
     * 程序名
     */
    private final String proName;

    public OplogNsTask(String dbTableWhite, OplogMetadata oplogMetadata, String proName) {
        this.dbTableWhite = dbTableWhite;
        this.oplogMetadata = oplogMetadata;
        this.proName = proName;
    }

    @Override
    public void run() {
        RealTimeTaskPoolManager.setRealTimeActiveThreadNum(proName, RealTimeTaskPoolManager.OPLOG_NS, 1);
        while (true) {
            // 要加上异常处理 以防出现解析ns的线程异常退出
            try {
                // 从原始的oplogList进行解析，获取对应的ns
                Document document = oplogMetadata.documentQueueOfOplog.poll();
                if (document != null) {
                    // 解析ns
                    parseNs(document);
                } else {
                    // 代表oplog队列为空 暂时休眠
                    TimeUnit.SECONDS.sleep(1);
                    // 进行判断此程序的状态
                    Integer proStatus = ProStatus.getProStatus(proName);
                    if (proStatus != ProStatus.REAL_TIME_RUN) {
                        // 此线程需要停止了
                        if (proStatus == ProStatus.PRO_STOP || proStatus == ProStatus.REAL_TIME_STOP) {
                            if (oplogMetadata.documentQueueOfOplog.size() == 0) {
                                Log.warn("程序:" + proName + ",检测到该实时程序进入STOP状态,分表线程即将关闭");
                                break;
                            }
                        } else if (proStatus == ProStatus.FULL_SYNC_RUN_AND_REAL_TIME_SLEEP || proStatus == ProStatus.REAL_TIME_SLEEP) {
                            // 睡眠前把内存数据退出去 以防占内存
                            if (oplogMetadata.documentQueueOfOplog.size() != 0) {
                                continue;
                            }
                            // 此线程需要进行睡眠
                            final Object proRealTimeObjectLock = ProStatus.getProRealTimeObjectLock(proName);
                            synchronized (proRealTimeObjectLock) {
                                Log.warn("程序:" + proName + ",检测到该实时程序进入SLEEP状态,分表线程即将睡眠");
                                proRealTimeObjectLock.wait();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.error("程序:" + proName + ",oplog的分表线程发生错误,错误信息:" + e.getMessage());
            }
        }
        RealTimeTaskPoolManager.setRealTimeActiveThreadNum(proName, RealTimeTaskPoolManager.OPLOG_NS, -1);
    }

    /**
     * parseNs
     *
     * @param document oplog信息
     * @desc 解析document的ns
     */
    public void parseNs(Document document) throws InterruptedException {
        String fullDbTableName = document.get("ns").toString();
        String op = document.get("op").toString();
        if ("c".equals(op)) {
            fullDbTableName = parseDDL(document);
            // 判读ddl的数据操作的ns是否符合表名过滤
            if (!fullDbTableName.matches(dbTableWhite)) {
                return;
            }
        }
        synchronized (oplogMetadata.syncObject) {
            // 判断下一层级nsBucket  dbTableQueueMap的是否包含对应的表名
            // dcl双重检查
            if (!oplogMetadata.dbTableQueueOfNsMap.containsKey(fullDbTableName)) {
                oplogMetadata.dbTableIsUseOfNsBucket.put(fullDbTableName, new AtomicBoolean());
                oplogMetadata.dbTableQueueOfNsMap.put(fullDbTableName, new LinkedBlockingQueue<>(oplogMetadata.getMaxTableQueueSizeOfNs()));
            }
            oplogMetadata.dbTableQueueOfNsMap.get(fullDbTableName).put(document);
        }

    }

    /**
     * parseDDL
     *
     * @param document
     * @desc 解析DDL document的ns
     */
    public String parseDDL(Document document) {
        Document o = (Document) document.get("o");
        String fullDbTableName = "";
        if (o.get("drop") != null) {
            fullDbTableName = parseDropTable(document);
        } else if (o.get("create") != null) {
            fullDbTableName = parseCreateTable(document);
        } else if (o.get("createIndexes") != null) {
            fullDbTableName = parseCreateIndex(document);
        } else if (o.get("commitIndexBuild") != null) {
            fullDbTableName = parseCommitIndexBuild(document);
        } else if (o.get("dropIndexes") != null) {
            fullDbTableName = parseDropIndex(document);
        } else if (o.get("renameCollection") != null) {
            fullDbTableName = parseRenameTable(document);
        } else if (o.get("convertToCapped") != null) {
            // fullDbTableName = parseRenameTable(document);
        } else if (o.get("dropDatabase") != null) {
            String ns = document.get("ns").toString();
            String[] nsSplit = ns.split("\\.", 2);
            fullDbTableName = nsSplit[0] + ".system.";
        }
        return fullDbTableName;
    }

    /**
     * parseDropTable
     *
     * @param document
     * @desc 解析DropTableDocument的ns
     */
    public String parseDropTable(Document document) {
        String ns = document.get("ns").toString();
        String[] nsSplit = ns.split("\\.", 2);
        String dbName = nsSplit[0];
        Document o = (Document) document.get("o");
        String tableName = o.get("drop").toString();
        return dbName + "." + tableName;
    }

    /**
     * parseCreateTable
     *
     * @param document
     * @desc 解析createTableDocument的ns
     */
    public String parseCreateTable(Document document) {
        String ns = document.get("ns").toString();
        String[] nsSplit = ns.split("\\.", 2);
        String dbName = nsSplit[0];
        Document o = (Document) document.get("o");
        String tableName = o.get("create").toString();
        return dbName + "." + tableName;
    }

    /**
     * parseRenameTable
     *
     * @param document
     * @desc 解析RenameTableDocument的ns
     */
    public String parseRenameTable(Document document) {
        String ns = document.get("ns").toString();
        String[] nsSplit = ns.split("\\.", 2);
        String dbName = nsSplit[0];
        Document o = (Document) document.get("o");
        String dbNameAndTableName = o.get("renameCollection").toString();
        String tableName = dbNameAndTableName.split("\\.")[1];
        return dbName + "." + tableName;
    }

    /**
     * parseCreateIndex
     *
     * @param document
     * @desc 解析CreateIndexDocument的ns
     */
    public String parseCreateIndex(Document document) {
        String ns = document.get("ns").toString();
        String[] nsSplit = ns.split("\\.", 2);
        String dbName = nsSplit[0];
        Document o = (Document) document.get("o");
        String tableName = o.get("createIndexes").toString();
        return dbName + "." + tableName;
    }

    public String parseCommitIndexBuild(Document document) {
        String ns = document.get("ns").toString();
        String[] nsSplit = ns.split("\\.", 2);
        String dbName = nsSplit[0];
        Document o = (Document) document.get("o");
        String tableName = o.get("commitIndexBuild").toString();
        return dbName + "." + tableName;
    }

    /**
     * parseDropIndex
     *
     * @param document
     * @desc 解析DropIndexDocument的ns
     */
    public String parseDropIndex(Document document) {
        String ns = document.get("ns").toString();
        String[] nsSplit = ns.split("\\.", 2);
        String dbName = nsSplit[0];
        Document o = (Document) document.get("o");
        String tableName = o.get("dropIndexes").toString();
        return dbName + "." + tableName;
    }
}
