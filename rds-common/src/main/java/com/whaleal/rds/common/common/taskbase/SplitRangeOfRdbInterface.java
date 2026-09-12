package com.whaleal.rds.common.common.taskbase;

import com.whaleal.rds.common.common.dataclass.Range;

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
     * @param sql
     * @return
     */
    List<Range> getRangeList(String dbTableName,String sql);
    /**
     * getRangeList
     *
     * @param dbTableName
     * @return
     */
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
