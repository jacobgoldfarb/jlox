package com.craftinginterpreters.lox.native_functions;

import java.util.List;

import com.craftinginterpreters.lox.*;

public class Clock implements LoxCallable {
    @Override
    public int arity() { return 0; }

    @Override
    public Object call(Interpreter interpreter,
                        List<Object> arguments) {
        return (double)System.currentTimeMillis() / 1000.0;
    }

    @Override
    public String toString() { return "<native fn>"; }
}