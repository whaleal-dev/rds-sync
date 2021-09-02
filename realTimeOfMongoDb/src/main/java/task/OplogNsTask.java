package task;

import common.OplogMetadata;
import org.bson.Document;
import util.Log;

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
    private OplogMetadata oplogMetadata;
    /**
     * 表过滤策略
     */
    private String dbTableWhite;

    public OplogNsTask(String dbTableWhite, OplogMetadata oplogMetadata) {
        this.dbTableWhite = dbTableWhite;
        this.oplogMetadata = oplogMetadata;
    }

    @Override
    public void run() {
        while (true) {
            try {
                // 从原始的OplogList进行解析，获取对应的ns
                Document document = oplogMetadata.documentQueue.poll();
                if (document != null) {
                    // 解析ns
                    parseNs(document);
                } else {
                    TimeUnit.SECONDS.sleep(1);
                    // Log.info(oplogMetadata.sourceDsName + ",OplogNsTaskWait");
                }
            } catch (Exception e) {
                Log.error(e.getMessage());
            }
        }
    }

    /**
     * parseNs
     *
     * @param document
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
        // 判断下一层级 dbTableQueueMap的是否包含对应的表名
        if (!oplogMetadata.dbTableQueueMap.containsKey(fullDbTableName)) {
            oplogMetadata.dbTableIsUseOfNsBucket.put(fullDbTableName, new AtomicBoolean());
            oplogMetadata.dbTableQueueMap.put(fullDbTableName, new LinkedBlockingQueue<>(oplogMetadata.getMaxTableQueueSize()));
        }
        oplogMetadata.dbTableQueueMap.get(fullDbTableName).put(document);
    }

    /**
     * parseDDL
     *
     * @param document
     * @desc 解析DDLdocument的ns
     */
    public String parseDDL(Document document) {
        Document o = (Document) document.get("o");
        String fullDbTableName = "";
        if (o.get("drop") != null) {
            fullDbTableName = parseDropTable(document);
        } else if (o.get("renameCollection") != null) {
            fullDbTableName = parseRenameTable(document);
        } else if (o.get("createIndexes") != null) {
            fullDbTableName = parseCreateIndex(document);
        } else if (o.get("dropIndexes") != null) {
            fullDbTableName = parseDropIndex(document);
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
        String fullDbTableName = dbName + "." + tableName;
        return fullDbTableName;
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
        String fullDbTableName = dbName + "." + tableName;
        return fullDbTableName;
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
        String fullDbTableName = dbName + "." + tableName;
        return fullDbTableName;
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
        String fullDbTableName = dbName + "." + tableName;
        return fullDbTableName;
    }
}
