crun: compile repl clean

run: repl clean


repl:
	java com.craftinginterpreters.lox.Lox

compile:
	javac com/craftinginterpreters/lox/Lox.java

gen expr:
	javac com/craftinginterpreters/tool/GenerateAst.java
	java com.craftinginterpreters.tool.GenerateAst com/craftinginterpreters/lox 

clean:
	rm com/craftinginterpreters/lox/*.class
	rm com/craftinginterpreters/tool/*.class