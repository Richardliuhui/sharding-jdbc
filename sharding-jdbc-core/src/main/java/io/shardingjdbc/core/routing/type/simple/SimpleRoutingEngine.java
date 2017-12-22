/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package io.shardingjdbc.core.routing.type.simple;

import io.shardingjdbc.core.api.algorithm.sharding.ShardingValue;
import io.shardingjdbc.core.hint.HintManagerHolder;
import io.shardingjdbc.core.hint.ShardingKey;
import io.shardingjdbc.core.parsing.parser.context.condition.Column;
import io.shardingjdbc.core.parsing.parser.context.condition.Condition;
import io.shardingjdbc.core.parsing.parser.sql.SQLStatement;
import io.shardingjdbc.core.routing.strategy.ShardingStrategy;
import io.shardingjdbc.core.routing.type.RoutingEngine;
import io.shardingjdbc.core.routing.type.RoutingResult;
import io.shardingjdbc.core.routing.type.TableUnit;
import io.shardingjdbc.core.rule.DataNode;
import io.shardingjdbc.core.rule.ShardingRule;
import io.shardingjdbc.core.rule.TableRule;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Simple routing engine.
 * 
 * @author zhangliang
 */
@RequiredArgsConstructor
public final class SimpleRoutingEngine implements RoutingEngine {
    
    private final ShardingRule shardingRule;
    
    private final List<Object> parameters;
    
    private final String logicTableName;
    
    private final SQLStatement sqlStatement;
    
    @Override
    public RoutingResult route() {
        TableRule tableRule = shardingRule.getTableRule(logicTableName);
        //获取分库的分片值
        List<ShardingValue> databaseShardingValues = getDatabaseShardingValues(tableRule);
        //获取分表的分片值
        List<ShardingValue> tableShardingValues = getTableShardingValues(tableRule);
        //分库路由
        Collection<String> routedDataSources = routeDataSources(tableRule, databaseShardingValues);
        Collection<DataNode> routedDataNodes = new LinkedList<>();
        for (String each : routedDataSources) {
            routedDataNodes.addAll(routeTables(tableRule, each, tableShardingValues));
        }
        return generateRoutingResult(routedDataNodes);
    }

    /***
     * 根据表规则获取库分片值
     * @param tableRule
     * @return
     */
    private List<ShardingValue> getDatabaseShardingValues(final TableRule tableRule) {
        ShardingStrategy strategy = shardingRule.getDatabaseShardingStrategy(tableRule);
        return HintManagerHolder.isUseShardingHint() ? getDatabaseShardingValuesFromHint(strategy.getShardingColumns()) : getShardingValues(strategy.getShardingColumns());
    }

    /***
     * 根据表规则获取表分片值
     * @param tableRule
     * @return
     */
    private List<ShardingValue> getTableShardingValues(final TableRule tableRule) {
        ShardingStrategy strategy = shardingRule.getTableShardingStrategy(tableRule);
        return HintManagerHolder.isUseShardingHint() ? getTableShardingValuesFromHint(strategy.getShardingColumns()) : getShardingValues(strategy.getShardingColumns());
    }
    
    private List<ShardingValue> getDatabaseShardingValuesFromHint(final Collection<String> shardingColumns) {
        List<ShardingValue> result = new ArrayList<>(shardingColumns.size());
        for (String each : shardingColumns) {
            Optional<ShardingValue> shardingValue = HintManagerHolder.getDatabaseShardingValue(new ShardingKey(logicTableName, each));
            if (shardingValue.isPresent()) {
                result.add(shardingValue.get());
            }
        }
        return result;
    }
    
    private List<ShardingValue> getTableShardingValuesFromHint(final Collection<String> shardingColumns) {
        List<ShardingValue> result = new ArrayList<>(shardingColumns.size());
        for (String each : shardingColumns) {
            Optional<ShardingValue> shardingValue = HintManagerHolder.getTableShardingValue(new ShardingKey(logicTableName, each));
            if (shardingValue.isPresent()) {
                result.add(shardingValue.get());
            }
        }
        return result;
    }
    
    private List<ShardingValue> getShardingValues(final Collection<String> shardingColumns) {
        List<ShardingValue> result = new ArrayList<>(shardingColumns.size());
        //循环所有分片列
        for (String each : shardingColumns) {
            Optional<Condition> condition = sqlStatement.getConditions().find(new Column(each, logicTableName));
            //如果存在condition,则获取分片值
            if (condition.isPresent()) {
                result.add(condition.get().getShardingValue(parameters));
            }
        }
        return result;
    }

    /***
     * 分库路由
     * @param tableRule
     * @param databaseShardingValues
     * @return
     */
    private Collection<String> routeDataSources(final TableRule tableRule, final List<ShardingValue> databaseShardingValues) {
        Collection<String> availableTargetDatabases = tableRule.getActualDatasourceNames();
        //如果分库分片值为空,则取默认的dataSource,此时的availableTargetDatabases就是默认的
        if (databaseShardingValues.isEmpty()) {
            return availableTargetDatabases;
        }
        //调用tableRule对应的分库策略获取分库值
        Collection<String> result = shardingRule.getDatabaseShardingStrategy(tableRule).doSharding(availableTargetDatabases, databaseShardingValues);
        Preconditions.checkState(!result.isEmpty(), "no database route info");
        return result;
    }

    /***
     * 分表路由
     * @param tableRule
     * @param routedDataSource
     * @param tableShardingValues
     * @return
     */
    private Collection<DataNode> routeTables(final TableRule tableRule, final String routedDataSource, final List<ShardingValue> tableShardingValues) {
        Collection<String> availableTargetTables = tableRule.getActualTableNames(routedDataSource);
        //如果分表的分片值为空,则取默认的表,反之则调用对应的分表策略获取分片值
        Collection<String> routedTables = tableShardingValues.isEmpty() ? availableTargetTables
                : shardingRule.getTableShardingStrategy(tableRule).doSharding(availableTargetTables, tableShardingValues);
        Preconditions.checkState(!routedTables.isEmpty(), "no table route info");
        Collection<DataNode> result = new LinkedList<>();
        for (String each : routedTables) {
            result.add(new DataNode(routedDataSource, each));
        }
        return result;
    }
    
    private RoutingResult generateRoutingResult(final Collection<DataNode> routedDataNodes) {
        RoutingResult result = new RoutingResult();
        for (DataNode each : routedDataNodes) {
            result.getTableUnits().getTableUnits().add(new TableUnit(each.getDataSourceName(), logicTableName, each.getTableName()));
        }
        return result;
    }
}
