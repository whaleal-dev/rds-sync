package common.taskbase;

import common.dataclass.Range;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @description:
 * @author: lhp
 * @time: 2021/9/6 8:07 下午
 */
public interface SplitRangeOfRdbInterface {
    /**
     * getIntMaxDifference
     *
     * @param intColumnName
     * @param dbTableName
     * @return
     */
    Map<String, Object> getIntMaxDifference(String intColumnName, String dbTableName);

    /**
     * getStringMaxDifference
     *
     * @param intColumnName
     * @param dbTableName
     * @return
     */
    Map<String, Object> getStringLengthMaxDifference(String intColumnName, String dbTableName);

    /**
     * getRangeList
     *
     * @param dbTableName
     * @return
     * @desc 1
     */
    List<Range> getRangeList(String dbTableName,String sql);

    List<Range> getRangeList(String dbTableName);
    /**
     * generateRangeListByIntColumn
     *
     * @param intColumnSet
     * @param dbTableName
     * @return
     */
    List<Range> generateRangeListByIntColumn(Set<String> intColumnSet, String dbTableName);

    /**
     * generateRangeListByStringColumn
     *
     * @param stringColumnSet
     * @param dbTableName
     * @return
     */
    List<Range> generateRangeListByStringColumn(Set<String> stringColumnSet, String dbTableName);


}
