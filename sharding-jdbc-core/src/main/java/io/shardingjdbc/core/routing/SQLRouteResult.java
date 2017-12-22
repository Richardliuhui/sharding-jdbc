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

package io.shardingjdbc.core.routing;

import io.shardingjdbc.core.parsing.parser.sql.SQLStatement;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * SQL route result.
 * 
 * @author gaohongtao
 * @author zhangliang
 */
@RequiredArgsConstructor
@Getter
public final class SQLRouteResult {
    /***
     * sql 解析的结果
     */
    private final SQLStatement sqlStatement;
    /**
     * 路由的后的SQLExecutionUnit
     */
    private final Set<SQLExecutionUnit> executionUnits = new LinkedHashSet<>();
    /***
     * 生成的主键列
     */
    private final List<Number> generatedKeys = new LinkedList<>();
}
