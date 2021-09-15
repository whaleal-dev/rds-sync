package common.taskbase;

/**
 * @description:
 * @author: lhp
 * @time: 2021/9/15 10:03 上午
 */
public class PhotonObject {

    /**
     * 任务名称
     */
    protected String taskName;
    /**
     * 程序名称
     */
    protected String proName;
    /**
     * 批次号
     */
    protected long batchNo;

    public PhotonObject(String taskName, String proName, long batchNo) {
        this.taskName = taskName;
        this.proName = proName;
        this.batchNo = batchNo;
    }

    public PhotonObject(String taskName, String proName) {
        this.taskName = taskName;
        this.proName = proName;
    }
}
