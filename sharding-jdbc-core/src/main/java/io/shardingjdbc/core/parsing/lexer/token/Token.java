package io.shardingjdbc.core.parsing.lexer.token;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Token.
 *
 * @author zhangliang
 */
@RequiredArgsConstructor
@Getter
public final class Token {
    /***
     * toker type 如table
     */
    private final TokenType type;
    /***
     * 文字
     */
    private final String literals;
    /***
     * 结束位置
     */
    private final int endPosition;
}
