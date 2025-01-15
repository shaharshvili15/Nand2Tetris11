import java.io.*;

public class CompilationEngine {
    private JackTokenizer tokenizer;
    private VMWriter vmWriter;
    private SymbolTable symbolTable;
    private String className;        // Current class name
    private String currentFunction;  // Current function/method name
    private int whileCounter;
    private int ifCounter;

    /**
     * Creates a new compilation engine.
     * The next routine called must be compileClass.
     * Receives the input file and the output file.
     */
    public CompilationEngine(File inputFile, File outputFile) throws IOException {
        tokenizer = new JackTokenizer(inputFile);
        vmWriter = new VMWriter(outputFile.getPath());
        symbolTable = new SymbolTable();
        whileCounter = 0;
        ifCounter = 0;

        if (tokenizer.hasMoreTokens()) {
            tokenizer.advance();
        } else {
            throw new IOException("Empty file: " + inputFile);
        }
    }

    /**
     * Compiles a complete class.
     */
    public void compileClass() throws IOException {
        // class className {
        handleKeyword(KeywordType.CLASS);
        className = tokenizer.identifier();
        handleIdentifier();
        handleSymbol('{');

        // Class variable declarations
        while (tokenizer.hasMoreTokens() && isClassVarDec()) {
            compileClassVarDec();
        }

        // Subroutine declarations
        while (tokenizer.hasMoreTokens() && isSubroutine()) {
            compileSubroutine();
        }

        //handleSymbol('}');
        vmWriter.close();
    }

    /**
     * Compiles a static declaration or a field declaration.
     */
    private void compileClassVarDec() throws IOException {
        // static or field
        String kind = tokenizer.keyword().toString();
        handleKeyword(KeywordType.STATIC, KeywordType.FIELD);

        String type = getType();

        while (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
            handleSymbol(',');
            compileVarName(type, kind);
        }

        handleSymbol(';');
    }

    /**
     * Compiles a complete method, function, or constructor.
     */
    private void compileSubroutine() throws IOException {
        symbolTable.reset();  // Reset subroutine-level symbol table

        // constructor/function/method
        KeywordType subroutineType = tokenizer.keyword();
        handleKeyword(KeywordType.CONSTRUCTOR, KeywordType.FUNCTION, KeywordType.METHOD);

        // Return type
        getType();  // void or type

        // subroutineName
        String functionName = tokenizer.identifier();
        currentFunction = className + "." + functionName;
        handleIdentifier();

        handleSymbol('(');

        // If it's a method, add 'this' as first argument
        if (subroutineType == KeywordType.METHOD) {
            symbolTable.define("this", className, "ARG");
        }

        compileParameterList();
        handleSymbol(')');

        // Subroutine body
        handleSymbol('{');

        // Local variables
        while (isVarDec()) {
            compileVarDec();
        }

        // Write function declaration
        vmWriter.writeFunction(currentFunction, symbolTable.varCount("VAR"));

        // If constructor, allocate memory for object
        if (subroutineType == KeywordType.CONSTRUCTOR) {
            vmWriter.writePush("constant", symbolTable.varCount("FIELD"));
            vmWriter.writeCall("Memory.alloc", 1);
            vmWriter.writePop("pointer", 0);
        }
        // If method, set up this pointer
        else if (subroutineType == KeywordType.METHOD) {
            vmWriter.writePush("argument", 0);
            vmWriter.writePop("pointer", 0);
        }

        compileStatements();
        handleSymbol('}');
    }

    /**
     * Compiles a (possibly empty) parameter list. Does not handle the enclosing parentheses.
     */
    private void compileParameterList() throws IOException {
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ')') {
            return;
        }

        // First parameter
        String type = getType();
        compileVarName(type, "ARG");

        // Additional parameters
        while (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
            handleSymbol(',');
            type = getType();
            compileVarName(type, "ARG");
        }
    }

    /**
     * Compiles a var declaration.
     */
    private void compileVarDec() throws IOException {
        handleKeyword(KeywordType.VAR);
        String type = getType();
        compileVarName(type, "VAR");

        while (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
            handleSymbol(',');
            compileVarName(type, "VAR");
        }

        handleSymbol(';');
    }

    /**
     * Compiles a sequence of statements. Does not handle the enclosing curly brackets.
     */
    private void compileStatements() throws IOException {
        while (true) {
            if (!tokenizer.hasMoreTokens()) break;

            TokenType type = tokenizer.tokenType();
            if (type != TokenType.KEYWORD) break;

            KeywordType keyword = tokenizer.keyword();
            switch (keyword) {
                case LET: compileLet(); break;
                case IF: compileIf(); break;
                case WHILE: compileWhile(); break;
                case DO: compileDo(); break;
                case RETURN: compileReturn(); break;
                default: return;
            }
        }
    }

    /**
     * Compiles a let statement.
     */
    private void compileLet() throws IOException {
        handleKeyword(KeywordType.LET);
        String varName = tokenizer.identifier();
        handleIdentifier();

        boolean isArray = false;
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '[') {
            isArray = true;
            handleSymbol('[');
            compileExpression();
            handleSymbol(']');

            // Push base address
            String kind = symbolTable.kindOf(varName);
            int index = symbolTable.indexOf(varName);
            vmWriter.writePush(segmentForKind(kind), index);

            // Add index to base address
            vmWriter.writeArithmetic("add");
        }

        handleSymbol('=');
        compileExpression();
        handleSymbol(';');

        if (isArray) {
            vmWriter.writePop("temp", 0);     // Save value
            vmWriter.writePop("pointer", 1);   // Set THAT
            vmWriter.writePush("temp", 0);     // Restore value
            vmWriter.writePop("that", 0);      // Store value
        } else {
            String kind = symbolTable.kindOf(varName);
            int index = symbolTable.indexOf(varName);
            vmWriter.writePop(segmentForKind(kind), index);
        }
    }

    /**
     * Compiles an if statement.
     */
    private void compileIf() throws IOException {
        String labelStart = "IF_" + ifCounter;
        String labelEnd = "IF_END_" + ifCounter;
        ifCounter++;

        handleKeyword(KeywordType.IF);
        handleSymbol('(');
        compileExpression();
        handleSymbol(')');

        vmWriter.writeIf(labelStart);    // If condition is true, skip the goto to else
        
        handleSymbol('{');
        compileStatements();
        handleSymbol('}');

        if (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyword() == KeywordType.ELSE) {
            vmWriter.writeGoto(labelEnd);
            vmWriter.writeLabel(labelStart);
            handleKeyword(KeywordType.ELSE);
            handleSymbol('{');
            compileStatements();
            handleSymbol('}');
            vmWriter.writeLabel(labelEnd);
        } else {
            vmWriter.writeLabel(labelStart);
        }
    }

    /**
     * Compiles a while statement.
     */
    private void compileWhile() throws IOException {
        String labelLoop = "WHILE_START" + whileCounter;
        String labelEnd = "WHILE_END" + whileCounter;
        whileCounter++;

        vmWriter.writeLabel(labelLoop);

        handleKeyword(KeywordType.WHILE);
        handleSymbol('(');
        compileExpression();
        handleSymbol(')');

        vmWriter.writeArithmetic("not");
        vmWriter.writeIf(labelEnd);

        handleSymbol('{');
        compileStatements();
        handleSymbol('}');

        vmWriter.writeGoto(labelLoop);
        vmWriter.writeLabel(labelEnd);
    }

    /**
     * Compiles a do statement.
     */
    private void compileDo() throws IOException {
        handleKeyword(KeywordType.DO);
        compileSubroutineCall();
        handleSymbol(';');
        vmWriter.writePop("temp", 0);  // Discard return value
    }

    /**
     * Compiles a return statement.
     */
    private void compileReturn() throws IOException {
        handleKeyword(KeywordType.RETURN);
        if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != ';') {
            compileExpression();
        } else {
            vmWriter.writePush("constant", 0);
        }
        handleSymbol(';');
        vmWriter.writeReturn();
    }

    /**
     * Compiles an expression.
     */
    private void compileExpression() throws IOException {
        compileTerm();

        while (tokenizer.tokenType() == TokenType.SYMBOL && isOperator(tokenizer.symbol())) {
            char operator = tokenizer.symbol();
            handleSymbol(operator);
            compileTerm();
            writeOperator(operator);
        }
    }

    /**
     * Compiles a term.
     */
    private void compileTerm() throws IOException {
        TokenType type = tokenizer.tokenType();

        switch (type) {
            case INT_CONST:
                vmWriter.writePush("constant", tokenizer.intVal());
                handleIntegerConstant();
                break;

            case STRING_CONST:
                String strVal = tokenizer.stringVal();
                vmWriter.writePush("constant", strVal.length());
                vmWriter.writeCall("String.new", 1);
                for (char c : strVal.toCharArray()) {
                    vmWriter.writePush("constant", (int)c);
                    vmWriter.writeCall("String.appendChar", 2);
                }
                handleStringConstant();
                break;

            case KEYWORD:
                KeywordType keyword = tokenizer.keyword();
                switch (keyword) {
                    case TRUE:
                        vmWriter.writePush("constant", 0);
                        vmWriter.writeArithmetic("not");
                        break;
                    case FALSE:
                        vmWriter.writePush("constant", 0);
                        break;
                    case NULL:
                        vmWriter.writePush("constant", 0);
                        break;
                    case THIS:
                        vmWriter.writePush("pointer", 0);
                        break;
                    default:
                        throw new IllegalArgumentException("Unexpected keyword in expression: " + keyword);
                }
                handleKeyword(keyword);
                break;

            case IDENTIFIER:
                String identifier = tokenizer.identifier();
                handleIdentifier();
                
                // Look ahead for '[', '(', or '.'
                if (tokenizer.hasMoreTokens() && tokenizer.tokenType() == TokenType.SYMBOL) {
                    char symbol = tokenizer.symbol();
                    if (symbol == '[') {
                        // Array access
                        handleSymbol('[');
                        compileExpression();
                        handleSymbol(']');

                        String kind = symbolTable.kindOf(identifier);
                        int index = symbolTable.indexOf(identifier);
                        vmWriter.writePush(segmentForKind(kind), index);
                        vmWriter.writeArithmetic("add");
                        vmWriter.writePop("pointer", 1);
                        vmWriter.writePush("that", 0);
                    } else if (symbol == '(' || symbol == '.') {
                        // Subroutine call
                        compileSubroutineCall(identifier);
                    } else {
                        // Variable
                        String kind = symbolTable.kindOf(identifier);
                        int index = symbolTable.indexOf(identifier);
                        vmWriter.writePush(segmentForKind(kind), index);
                    }
                } else {
                    // Variable
                    String kind = symbolTable.kindOf(identifier);
                    int index = symbolTable.indexOf(identifier);
                    vmWriter.writePush(segmentForKind(kind), index);
                }
                break;

            case SYMBOL:
                char symbol = tokenizer.symbol();
                if (symbol == '(') {
                    // Parenthesized expression
                    handleSymbol('(');
                    compileExpression();
                    handleSymbol(')');
                } else if (symbol == '-' || symbol == '~') {
                    // Unary operation
                    handleSymbol(symbol);
                    compileTerm();
                    if (symbol == '-') {
                        vmWriter.writeArithmetic("neg");
                    } else {
                        vmWriter.writeArithmetic("not");
                    }
                }
                break;
        }
    }

    /**
     * Compiles a subroutine call.
     */
    private void compileSubroutineCall() throws IOException {
        String identifier = tokenizer.identifier();
        handleIdentifier();
        compileSubroutineCall(identifier);
    }

    private void compileSubroutineCall(String identifier) throws IOException {
        int nArgs = 0;
        
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '.') {
            handleSymbol('.');
            String methodName = tokenizer.identifier();
            handleIdentifier();
            
            // Check if identifier is a variable name in our symbol table
            String kind = symbolTable.kindOf(identifier);
            if (!kind.equals("NONE")) {
                // Case 1: object.method()
                String className = symbolTable.typeOf(identifier);
                vmWriter.writePush(kind.toLowerCase(), symbolTable.indexOf(identifier));
                nArgs = 1;  // this object is first argument
                vmWriter.writeCall(className + "." + methodName, nArgs + compileExpressionList());
            } else {
                // Case 2: Class.function()
                vmWriter.writeCall(identifier + "." + methodName, compileExpressionList());
            }
        } else {
            // Case 3: method() - method call within same class
            vmWriter.writePush("pointer", 0);  // push this
            nArgs = 1;
            vmWriter.writeCall(className + "." + identifier, nArgs + compileExpressionList());
        }
    }

    /**
     * Compiles a (possibly empty) comma-separated list of expressions.
     * Returns the number of expressions in the list.
     */
    private int compileExpressionList() throws IOException {
        int nArgs = 0;

        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ')') {
            return nArgs;
        }

        compileExpression();
        nArgs = 1;

        while (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
            handleSymbol(',');
            compileExpression();
            nArgs++;
        }

        return nArgs;
    }


    // Helper methods :

    private void handleKeyword(KeywordType... expected) throws IOException {
        if (tokenizer.tokenType() != TokenType.KEYWORD) {
            throw new IllegalStateException("Expected keyword, got " + tokenizer.tokenType());
        }

        KeywordType currentKeywordType = tokenizer.keyword();
        boolean found = false;
        for (KeywordType exp : expected) {
            if (currentKeywordType == exp) {
                found = true;
                break;
            }
        }

        if (!found) {
            throw new IllegalStateException("Expected one of the keywords: " + expected + ", got " + currentKeywordType);
        }

        tokenizer.advance();
    }

    private void handleSymbol(char expected) throws IOException {
        if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != expected) {
            throw new IllegalStateException("Expected symbol '" + expected + "', got " + tokenizer.tokenType());
        }
        tokenizer.advance();
    }

    private void handleIdentifier() throws IOException {
        if (tokenizer.tokenType() != TokenType.IDENTIFIER) {
            throw new IllegalStateException("Expected identifier, got " + tokenizer.tokenType());
        }
        tokenizer.advance();
    }

    private void handleIntegerConstant() throws IOException {
        if (tokenizer.tokenType() != TokenType.INT_CONST) {
            throw new IllegalStateException("Expected integer constant, got " + tokenizer.tokenType());
        }
        tokenizer.advance();
    }

    private void handleStringConstant() throws IOException {
        if (tokenizer.tokenType() != TokenType.STRING_CONST) {
            throw new IllegalStateException("Expected string constant, got " + tokenizer.tokenType());
        }
        tokenizer.advance();
    }

    private String getType() throws IOException {
        String type;
        if (tokenizer.tokenType() == TokenType.KEYWORD) {
            type = tokenizer.keyword().toString();
            handleKeyword(KeywordType.INT, KeywordType.CHAR, KeywordType.BOOLEAN, KeywordType.VOID);
        } else {
            type = tokenizer.identifier();
            handleIdentifier();
        }
        return type;
    }

    private boolean isClassVarDec() throws IOException {
        return tokenizer.tokenType() == TokenType.KEYWORD &&
               (tokenizer.keyword() == KeywordType.STATIC ||
                tokenizer.keyword() == KeywordType.FIELD);
    }

    private boolean isSubroutine() throws IOException {
        return tokenizer.tokenType() == TokenType.KEYWORD &&
               (tokenizer.keyword() == KeywordType.CONSTRUCTOR ||
                tokenizer.keyword() == KeywordType.FUNCTION ||
                tokenizer.keyword() == KeywordType.METHOD);
    }

    private boolean isVarDec() throws IOException {
        return tokenizer.tokenType() == TokenType.KEYWORD &&
               tokenizer.keyword() == KeywordType.VAR;
    }

    private boolean isOperator(char c) {
        return "+-*/&|<>=".indexOf(c) != -1;
    }

    private void writeOperator(char operator) {
        switch (operator) {
            case '+': vmWriter.writeArithmetic("add"); break;
            case '-': vmWriter.writeArithmetic("sub"); break;
            case '*': vmWriter.writeCall("Math.multiply", 2); break;
            case '/': vmWriter.writeCall("Math.divide", 2); break;
            case '&': vmWriter.writeArithmetic("and"); break;
            case '|': vmWriter.writeArithmetic("or"); break;
            case '<': vmWriter.writeArithmetic("lt"); break;
            case '>': vmWriter.writeArithmetic("gt"); break;
            case '=': vmWriter.writeArithmetic("eq"); break;
        }
    }

    private String segmentForKind(String kind) {
        switch (kind) {
            case "STATIC": return "static";
            case "FIELD": return "this";
            case "ARG": return "argument";
            case "VAR": return "local";
            default: throw new IllegalArgumentException("Invalid kind: " + kind);
        }
    }

    private void compileVarName(String type, String kind) throws IOException {
        String name = tokenizer.identifier();
        symbolTable.define(name, type, kind);
        handleIdentifier();
    }
}
