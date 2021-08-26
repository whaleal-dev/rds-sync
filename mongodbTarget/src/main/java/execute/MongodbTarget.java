package execute;

import cache.MemoryCache;
import conf.Configuration;
import lombok.AllArgsConstructor;
import task.MongodbTargetTask;
import thread.TargetTaskPoolManager;


/**
 * @author: lhp
 * @time: 2021/7/19 3:02 下午
 * @desc: 主类
 */
@AllArgsConstructor
public class MongodbTarget {
    private Configuration configuration;
    private MemoryCache memoryCache;
    private String procName;

    public void startToTarget() {
        for (int i = 0; i < configuration.getTargetThreadNum(); i++) {
            TargetTaskPoolManager.submit(procName, new MongodbTargetTask(configuration, memoryCache));
        }

    }
}
