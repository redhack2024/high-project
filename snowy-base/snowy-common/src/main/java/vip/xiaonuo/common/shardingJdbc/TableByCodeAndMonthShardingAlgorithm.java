package com.ruoyi.framework.shardingJdbc;


import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import com.google.common.collect.Range;
import org.apache.shardingsphere.api.sharding.complex.ComplexKeysShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.complex.ComplexKeysShardingValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class TableByCodeAndMonthShardingAlgorithm implements ComplexKeysShardingAlgorithm {
    private static final Logger logger = LoggerFactory.getLogger(TableByCodeAndMonthShardingAlgorithm.class);
    String dateField;
    String subNameField;

    public TableByCodeAndMonthShardingAlgorithm(){
        this.dateField = "inspection_time";
        this.subNameField = "tenantCode";
    }
    public TableByCodeAndMonthShardingAlgorithm(String dateField, String subNameField){
        this.dateField = dateField;
        this.subNameField = subNameField;
    }

    @Override
    public Collection<String> doSharding(Collection availableTargetNames, ComplexKeysShardingValue complexKeysShardingValue) {
        Map<String, Collection> columnNameAndShardingValuesMap = complexKeysShardingValue.getColumnNameAndShardingValuesMap();
        ArrayList<String> monthList = new ArrayList<>();
        ArrayList<String> codeList = new ArrayList<>();
        ArrayList<String> columnNameAndShardingList = new ArrayList<>();

        ArrayList<String> tables = new ArrayList<>();

        //精确分片
        if (CollectionUtil.isNotEmpty(columnNameAndShardingValuesMap)) {
            Collection<Date> dateCollection = (Collection) columnNameAndShardingValuesMap.get(this.dateField);

            if (CollectionUtil.isEmpty(dateCollection)) {
                dateCollection = (Collection) columnNameAndShardingValuesMap.get("create_time");
            }
            if (CollectionUtil.isNotEmpty(dateCollection)) {
                for (Date date : dateCollection) {
                    monthList.add(DateUtil.year(date) + "_" + (DateUtil.month(date) + 1));
                }
            }

            Collection<String> insurCompanyCodeList = (Collection) columnNameAndShardingValuesMap.get(this.subNameField);
            if (CollectionUtil.isEmpty(insurCompanyCodeList)) {
                //insurCompanyCodeList = (Collection) columnNameAndShardingValuesMap.get(this.typeNameField);
                insurCompanyCodeList = Collections.singleton("pengzhourenyi");
            }
            //再通过业务code过滤
            if (CollectionUtil.isNotEmpty(insurCompanyCodeList)) {
                for (String code : insurCompanyCodeList) {
                    codeList.add(code);
                }
            }

            if (CollectionUtil.isNotEmpty(codeList) && CollectionUtil.isNotEmpty(monthList)) {
                for (String code : codeList) {
                    for (String month : monthList) {
                        columnNameAndShardingList.add(code + "_" + month);
                    }
                }
            } else if (CollectionUtil.isNotEmpty(codeList)) {

                for (String code : codeList) {
                    columnNameAndShardingList.add(code);
                }
            } else if (CollectionUtil.isNotEmpty(monthList)) {
                for (String month : monthList) {
                    columnNameAndShardingList.add(month);
                }
            } else {
                logger.error("未按分片键查询,不支持此类操作");
                throw new UnsupportedOperationException();
            }

            if (CollectionUtil.isNotEmpty(columnNameAndShardingList)) {
                for (Object tableName : availableTargetNames) {
                    if (tableName == null) {
                        continue;
                    }
                    for (String target : columnNameAndShardingList) {
                        String lowerCase = target.toLowerCase();
                        if (tableName.toString().contains(lowerCase)) {
                            tables.add(tableName.toString());
                            break;
                        }
                    }
                }
            }
        }

        Map<String, Range<Date>> columnNameAndRangeValuesMap = complexKeysShardingValue.getColumnNameAndRangeValuesMap();
        if (CollectionUtil.isNotEmpty(columnNameAndRangeValuesMap)) {
            Range<Date> dateRange = columnNameAndRangeValuesMap.get(this.dateField);
            Date start = dateRange.lowerEndpoint();
            Date end = dateRange.upperEndpoint();
            int startYear = DateUtil.year(start);
            int endYear = DateUtil.year(end);

            int startMonth = DateUtil.month(start) + 1;
            int endMonth = DateUtil.month(end) + 1;

            if (startYear > endYear) {
                throw new UnsupportedOperationException();
            }
            List<String> finalTables = null;
            //通过精确分片过滤的数据库表集合不为空
            if (CollectionUtil.isNotEmpty(tables)) {
                finalTables = getResults(tables, startYear, endYear, startMonth, endMonth);
            } else {
                for (Object targetName : availableTargetNames) {
                    if (targetName == null) {
                        continue;
                    }
                    tables.add(targetName.toString());
                }
                //无精确分片过滤的表集合
                finalTables = getResults(tables, startYear, endYear, startMonth, endMonth);
            }
            return finalTables;
        }
        return tables;
    }

    private List<String> getResults(ArrayList<String> tables, int startYear, int endYear, int startMonth, int endMonth) {
        List<String> finalTables = new ArrayList<>();
        if (startYear == endYear) {
            //同年
            for (String tableName : tables) {
                for (int i = startMonth; i <= endMonth; i++) {
                    if (tableName.endsWith("_" + i)) {
                        finalTables.add(tableName);
                        break;
                    }
                }
            }
            return finalTables;
        } else if ((endYear - startYear) == 1) {
            //跨一年
            String tableNameOne = tables.get(0);
            if (tableNameOne.contains(startYear + "")) {
                for (String tableName : tables) {
                    for (int i = startMonth; i <= 12; i++) {
                        if (tableName.endsWith("_" + i)) {
                            finalTables.add(tableName);
                            break;
                        }
                    }
                }
                return finalTables;
            } else if (tableNameOne.contains(endYear + "")) {
                for (String tableName : tables) {
                    for (int i = 1; i <= endMonth; i++) {
                        if (tableName.endsWith("_" + i)) {
                            finalTables.add(tableName);
                            break;
                        }
                    }
                }
                return finalTables;
            }
        } else {
            //跨两年及以上
            logger.error("跨两年及以上的多数据库查询，影响性能，暂不支持此操作");
            throw new UnsupportedOperationException();
        }
        return null;
    }
}
