package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static com.craftinginterpreters.lox.TokenType.*;

class Parser {
    private static class ParseError extends RuntimeException {}


    private final List<Token> tokens;
    private int current_token_idx = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    private Stmt declaration() {
        try {
            if (match_any(VAR)) return varDeclaration();
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }
    
    private Stmt statement() {
        if (match_any(IF)) return ifStatement();
        if (match_any(FOR)) return forStatement();
        if (match_any(WHILE)) return whileStatement();
        if (match_any(PRINT)) return printStatement();
        if (match_any(LEFT_BRACE)) return new Stmt.Block(block());
        
        return expressionStatement();
    }

    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Stmt initializer;
        if (match_any(SEMICOLON)) {
            initializer = null;
        } else if (match_any(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!match_one(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition.");

        Expr increment = null;
        if (!match_one(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after loop condition.");
        Stmt body = statement();

        // If there is an increment then replace the body with a block that executes the body
        // followed by the increment expression.
        if (increment != null) {
            body = new Stmt.Block(
                Arrays.asList(
                    body,
                    new Stmt.Expression(increment)
                )
            );
        }

        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        // If there is an initializer, then wrap the Stmt.While statement in a block that
        // executes the initializer once before the body.
        if (initializer != null) {
            body = new Stmt.Block(
                Arrays.asList(
                    initializer,
                    body
                )
            );
        }

        return body;
    }

    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after 'if' condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match_any(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after 'if' condition.");

        Stmt body = statement();

        return new Stmt.While(condition, body);
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume_semicolon();
        return new Stmt.Print(value);
    }

    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");
        
        Expr initializer = null;
        if (match_any(EQUAL)) {
            initializer = expression();
        }
        
        consume_semicolon();
        return new Stmt.Var(name, initializer);
    }
    
    private Stmt expressionStatement() {
        Expr value = expression();
        consume_semicolon();
        return new Stmt.Expression(value);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!match_one(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        // l-value, allows for complex assignment like `makeList(a - 3).head.next = node;`
        Expr expr = or();

        if (match_any(EQUAL)) {
            Token equals = previous();
            // r-value
            Expr value = assignment();

            // Check if l-value is Expr.Variable (identifier)
            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    private Expr or() {
        Expr expr = and();
        while (match_any(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr; 
    }

    private Expr and() {
        Expr expr = equality();
        while (match_any(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }
    
    private Expr equality() {
        Expr expr = comparison();

        while (match_any(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison() {
        Expr expr = term();

        while (match_any(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() {
        Expr expr = factor();

        while (match_any(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        Expr expr = unary();

        while (match_any(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }
    
    private Expr unary() {
        if (match_any(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return primary();
    }

    private Expr primary() {
        if (match_any(FALSE)) return new Expr.Literal(false);
        if (match_any(TRUE)) return new Expr.Literal(true);
        if (match_any(NIL)) return new Expr.Literal(null);

        if (match_any(NUMBER, STRING)) {
            Token value = previous();
            return new Expr.Literal(value.literal);
        } 

        if (match_any(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match_any(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
    }

    /// Advances cursor on match
    private boolean match_any(TokenType... types) {
        for (TokenType type: types) {
            if (match_one(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean match_one(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token consume(TokenType type, String message) {
        if (match_one(type)) return advance();
        throw error(peek(), message);
    }

    private Token consume_semicolon() {
        // return advance();
        // uncomment to enable semicolons.
        return consume(SEMICOLON, "Expect ';' after value");
    }

    private Token advance() {
        if (!isAtEnd()) current_token_idx++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current_token_idx);
    }

    private Token previous() {
        return tokens.get(current_token_idx - 1);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;
            // All the token types which indicate the start of a new statement
            switch (peek().type) {
                case CLASS: case FOR: case FUN: case IF: case PRINT:
                case RETURN: case VAR: case WHILE:
                return;
            }
            advance();
        }
    }

}