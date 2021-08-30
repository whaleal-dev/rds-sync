package common.taskbase;

/**
 * @description:
 * @author: lhp
 * @time: 2021/8/24 12:41 下午
 */
public interface SourceTaskInterface {


    /**
     * getDataFromCollection 获取表数据
     *
     * @desc 获取表数据
     */
    void getDataFromCollection();

    /**
     * putDataToCache 放数据到缓存对象中
     *
     * @desc 放数据到缓存对象中
     */
    void putDataToCache();
}
