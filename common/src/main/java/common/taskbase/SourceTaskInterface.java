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

    void dataTransformation(Object object);

    void putDataToCache();
}
