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

package io.shardingjdbc.core.parsing.parser.sql.dml.insert;

import io.shardingjdbc.core.rule.ShardingRule;
import io.shardingjdbc.core.parsing.lexer.LexerEngine;
import io.shardingjdbc.core.parsing.lexer.token.DefaultKeyword;
import io.shardingjdbc.core.parsing.lexer.token.Symbol;
import io.shardingjdbc.core.parsing.parser.clause.facade.AbstractInsertClauseParserFacade;
import io.shardingjdbc.core.parsing.parser.sql.SQLParser;
import io.shardingjdbc.core.parsing.parser.sql.dml.DMLStatement;
import io.shardingjdbc.core.parsing.parser.token.GeneratedKeyToken;
import io.shardingjdbc.core.parsing.parser.token.ItemsToken;
import com.google.common.base.Optional;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * Insert parser.
 *
 * @author zhangliang
 */
public abstract class AbstractInsertParser implements SQLParser {
    
    @Getter(AccessLevel.PROTECTED)
    private final ShardingRule shardingRule;
    
    @Getter(AccessLevel.PROTECTED)
    private final LexerEngine lexerEngine;
    
    private final AbstractInsertClauseParserFacade insertClauseParserFacade;
    
    public AbstractInsertParser(final ShardingRule shardingRule, final LexerEngine lexerEngine, final AbstractInsertClauseParserFacade insertClauseParserFacade) {
        this.shardingRule = shardingRule;
        this.lexerEngine = lexerEngine;
        this.insertClauseParserFacade = insertClauseParserFacade;
    }

    /***
     * insert sql 解析
     * @return
     */
    @Override
    public final DMLStatement parse() {
        lexerEngine.nextToken();
        InsertStatement result = new InsertStatement();
        //表名解析
        insertClauseParserFacade.getInsertIntoClauseParser().parse(result);
        //列名解析
        insertClauseParserFacade.getInsertColumnsClauseParser().parse(result);
        if (lexerEngine.equalAny(DefaultKeyword.SELECT, Symbol.LEFT_PAREN)) {
            throw new UnsupportedOperationException("Cannot INSERT SELECT");
        }
        //解析values
        insertClauseParserFacade.getInsertValuesClauseParser().parse(result);
        insertClauseParserFacade.getInsertSetClauseParser().parse(result);
        //处理生成列
        appendGenerateKey(result);
        return result;
    }
    
    private void appendGenerateKey(final InsertStatement insertStatement) {
        String tableName = insertStatement.getTables().getSingleTableName();
        Optional<String> generateKeyColumn = shardingRule.getGenerateKeyColumn(tableName);
        if (!generateKeyColumn.isPresent() || null != insertStatement.getGeneratedKey()) {
            return;
        } 
        ItemsToken columnsToken = new ItemsToken(insertStatement.getColumnsListLastPosition());
        columnsToken.getItems().add(generateKeyColumn.get());
        //把主键生成的列放入到现有列的最后位置,添加到sqlTokens
        insertStatement.getSqlTokens().add(columnsToken);
        //把主键列的?放到所有?的最后面 如insert into t_order(user_id,status,order_id) values(?,?,?)
        insertStatement.getSqlTokens().add(new GeneratedKeyToken(insertStatement.getValuesListLastPosition()));
    }
}
