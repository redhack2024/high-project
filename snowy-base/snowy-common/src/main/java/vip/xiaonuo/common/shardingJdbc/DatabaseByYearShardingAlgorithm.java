package com.ruoyi.framework.shardingJdbc;

import cn.hutool.core.date.DateUtil;
import org.apache.shardingsphere.api.sharding.standard.PreciseShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.standard.PreciseShardingValue;

import java.util.Collection;
import java.util.Date;
import java.util.Objects;

/**
* @desc 按年分库精确分片算法
*/
public class DatabaseByYearShardingAlgorithm implements PreciseShardingAlgorithm<Date> {
    @Override
    public String doSharding(Collection<String> collection, PreciseShardingValue<Date> preciseShardingValue) {
        Date createTime = preciseShardingValue.getValue();
        int year = DateUtil.year(createTime);
        for (String database : collection) {
            if (Objects.equals(database, String.valueOf(year))) {
                return database;
            }
            if(database.endsWith(String.valueOf(year))){
                return database;
            }
        }
        throw new UnsupportedOperationException();
    }
}
