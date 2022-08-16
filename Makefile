run: repl clean

repl:
	javac com/craftinginterpreters/lox/Lox.java
	java com.craftinginterpreters.lox.Lox

gen expr:
	javac com/craftinginterpreters/tool/GenerateAst.java
	java com.craftinginterpreters.tool.GenerateAst com/craftinginterpreters/lox 

clean:
	rm com/craftinginterpreters/lox/*.class
	rm com/craftinginterpreters/tool/*.class