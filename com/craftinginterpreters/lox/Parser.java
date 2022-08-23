package com.craftinginterpreters.lox;

import java.util.ArrayList;
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

    private Expr expression() {
        return equality();
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
        if (match_any(PRINT)) return printStatement();

        return expressionStatement();
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
        return advance();
        // uncomment to enable semicolons.
        // return consume(SEMICOLON, "Expect ';' after value");
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