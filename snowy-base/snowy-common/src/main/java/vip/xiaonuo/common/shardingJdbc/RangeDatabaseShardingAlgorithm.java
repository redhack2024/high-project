package com.ruoyi.framework.shardingJdbc;


import cn.hutool.core.date.DateUtil;
import com.google.common.collect.Range;
import org.apache.shardingsphere.api.sharding.standard.RangeShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.standard.RangeShardingValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * @Description: 数据库范围分片算法
 */
public class RangeDatabaseShardingAlgorithm implements RangeShardingAlgorithm<Date> {

    /**
     * 范围分片算法
     *
     * @param availableTargetNames 所有配置的库列表
     * @param rangeShardingValue   分片值，也就是createTime的值，范围分片算法必须提供开始时间和结束时间
     * @return 所匹配库的结果
     */
    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, RangeShardingValue<Date> rangeShardingValue) {
        final ArrayList<String> result = new ArrayList<>();
        Range<Date> range = rangeShardingValue.getValueRange();
        Date startTime = range.lowerEndpoint();
        Date endTime = range.upperEndpoint();

        // 起始年和结束年
        int startYear = DateUtil.year(startTime);
        int endYear = DateUtil.year(endTime);

        Collection<String> databaseList =  startYear == endYear ? theSameYear(String.valueOf(startYear), availableTargetNames, result) : differentYear(startYear, endYear, availableTargetNames, result);
        return databaseList;
    }

    // 起始年跟结束年在同一年，只需要查一个数据库
    private Collection<String> theSameYear(String startTime, Collection<String> availableTargetNames, ArrayList<String> result) {
        for (String availableTargetName : availableTargetNames) {
            if (availableTargetName.endsWith(startTime)) {
                result.add(availableTargetName);
            }
        }
        return result;
    }

    // 跨年，查多个数据库
    private Collection<String> differentYear(int startYear, int endYear, Collection<String> availableTargetNames, ArrayList<String> result) {
        for (String availableTargetName : availableTargetNames) {
            for (int i = startYear; i <= endYear; i++) {
                if (availableTargetName.endsWith(String.valueOf(i))) {
                    result.add(availableTargetName);
                }
            }
        }
        return result;
    }
}