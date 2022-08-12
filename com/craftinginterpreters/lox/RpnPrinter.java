package com.craftinginterpreters.lox;

import java.util.List;
import java.util.ArrayList;

class RpnPrinter implements Expr.Visitor<String> {

    private final List<Token> operators = new ArrayList<>();
    private final List<Token> literals = new ArrayList<>();

    String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        StringBuilder builder = new StringBuilder();
        builder.append(expr.left.accept(this));
        builder.append(" ");
        builder.append(expr.right.accept(this));
        builder.append(" ");
        builder.append(expr.operator.lexeme);
        builder.append(" ");
        return builder.toString();
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        StringBuilder builder = new StringBuilder();
        builder.append(expr.expression.accept(this));
        builder.append(" ");
        return builder.toString();
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        StringBuilder builder = new StringBuilder();
        builder.append(expr.value);
        builder.append(" ");
        return builder.toString();
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        StringBuilder builder = new StringBuilder();
        builder.append(expr.operator.lexeme);
        builder.append(" ");
        builder.append(expr.right.accept(this));
        builder.append(" ");
        return builder.toString();
    }

    public static void main(String[] args) {
        // (1 + 2) * (4 - 3)
        Expr expression = new Expr.Binary(
            new Expr.Grouping(
                new Expr.Binary(
                    new Expr.Literal(1),
                    new Token(TokenType.PLUS, "+", null, 1),
                    new Expr.Literal(2)
                )
            ),
            new Token(TokenType.STAR, "*", null, 1),
            new Expr.Grouping(
                new Expr.Binary(
                    new Expr.Literal(4),
                    new Token(TokenType.MINUS, "-", null, 1),
                    new Expr.Literal(3)
                )
            )
        );
        System.out.println(new RpnPrinter().print(expression));
    }
}