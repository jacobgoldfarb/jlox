package com.craftinginterpreters.lox.native_functions;
import java.util.List;

import com.craftinginterpreters.lox.*;

public class Print implements LoxCallable {
    @Override
    public int arity() { return 1; }
    
    @Override
    public Object call(Interpreter interpreter,
                        List<Object> arguments) {
        String output = String.valueOf(arguments.get(0));
        System.out.println(output);
        return null;
    }
    
    @Override
    public String toString() { return "<native fn>"; }
}
