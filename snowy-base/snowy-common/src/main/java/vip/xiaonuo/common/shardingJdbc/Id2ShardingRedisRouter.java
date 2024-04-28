package com.ruoyi.framework.shardingJdbc;

import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.Date;
import java.util.Map;

public class Id2ShardingRedisRouter {
    private static final Logger logger = LoggerFactory.getLogger(Id2ShardingRedisRouter.class);

    public static void updateShardingMaxId(Long primaryKey, Date shardingDate,String logicTableName){
        RedisCache redisCache = SpringUtils.getBean("redisCache");
        String year = DateUtils.getYear(shardingDate)+"";
        //String key = logicTableName+"_"+year;
        Object cacheMapValue = redisCache.getCacheMapValue(logicTableName, year);
        if(cacheMapValue==null){
            redisCache.setCacheMapValue(logicTableName,year,primaryKey);
            return;
        }
        Long currentId = (Long)cacheMapValue;
        if(primaryKey>currentId){
            redisCache.setCacheMapValue(logicTableName,year,primaryKey);
        }
    }

    public static Date queryShardingNameById(Long primaryKey, String logicTableName){
        RedisCache redisCache = SpringUtils.getBean("redisCache");
        Date shardingDate = null;
        Map<String, Object> cacheMap = redisCache.getCacheMap(logicTableName);
        if(cacheMap==null || cacheMap.size()<=0){
            return null;
        }

        String shardingKey = null;
        for (String key : cacheMap.keySet()) {
            Object id_inRedis = cacheMap.get(key);
            if(id_inRedis==null){
                continue;
            }
            Long currentMaxId = (Long) id_inRedis;
            //如果第一次找到key，则取这个key， 如果找到更小的key则用更小的
            if(primaryKey<currentMaxId && (shardingKey==null || shardingKey.compareTo(key)>0)){
                shardingKey = key;
            }
        }

        if(shardingKey==null){
            return null;
        }

        try {
            shardingDate = DateUtils.parseDate(shardingKey,"yyyy");
            return shardingDate;
        } catch (ParseException e) {
            logger.error("获取分片对应的时间错误queryShardingNameById",e);
            return null;
        }

    }

}
