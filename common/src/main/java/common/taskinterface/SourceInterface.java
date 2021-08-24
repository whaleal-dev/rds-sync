package common.taskinterface;

import org.bson.Document;

/**
 * @description:
 * @author: lhp
 * @time: 2021/8/24 12:41 下午
 */
public interface SourceInterface {


    /**
     * getDataFromCollection 获取表数据
     *
     * @desc 获取表数据
     */
    public void getDataFromCollection();

    public void dataTransformation(Object object);

    public void putDataToCache();
}
