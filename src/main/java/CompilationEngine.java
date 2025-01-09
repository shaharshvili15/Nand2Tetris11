import java.io.*;

public class CompilationEngine {
    private JackTokenizer tokenizer;
    private PrintWriter writer;
    private int indentLevel;

    /**
     * Creates a new compilation engine.
     * The next routine called must be compileClass.
     */
    public CompilationEngine(File inputFile, File outputFile) throws IOException {
        tokenizer = new JackTokenizer(inputFile);
        writer = new PrintWriter(new FileWriter(outputFile));
        indentLevel = 0;
    }

    /**
     * Compiles a complete class.
     */
    public void compileClass() throws IOException {
        openExp("class");
        
        handleKeyword(KeywordType.CLASS);
        handleIdentifier(); // class name
        handleSymbol('{');

        while (tokenizer.hasMoreTokens()) {
            if (isClassVarDec()) {
                compileClassVarDec();
            } else if (isSubroutine()) {
                compileSubroutine();
            } else {
                handleSymbol('}');
                break;
            }
        }

        closeExp("class");
        writer.close();
    }

    /**
     * Compiles a static variable declaration or field declaration.
     */
    public void compileClassVarDec() throws IOException {
        openExp("classVarDec");
        
        // static or field
        handleKeyword(KeywordType.STATIC, KeywordType.FIELD);
        
        // type and var names
        compileType();
        compileVarName();
        
        // additional var names
        while (tokenizer.symbol() == ',') {
            handleSymbol(',');
            compileVarName();
        }
        
        handleSymbol(';');
        
        closeExp("classVarDec");
    }

    /**
     * Compiles a complete method, function, or constructor.
     */
    public void compileSubroutine() throws IOException {
        // subroutine declaration
        handleKeyword(KeywordType.CONSTRUCTOR, KeywordType.FUNCTION, KeywordType.METHOD);
        
        // return type
        if (tokenizer.tokenType() == TokenType.KEYWORD) {
            handleKeyword(KeywordType.VOID, KeywordType.INT, KeywordType.BOOLEAN, KeywordType.CHAR);
        } else {
            handleIdentifier(); // class/object name as return type
        }
        
        handleIdentifier(); // subroutine name
        handleSymbol('(');
        compileParameterList();
        handleSymbol(')');
        
        compileSubroutineBody();
    }

    /**
     * Compiles a (possibly empty) parameter list. Does not handle the enclosing parentheses.
     */
    public void compileParameterList() throws IOException {
        if (tokenizer.symbol() == ')') {
            return; // empty parameter list
        }

        // First parameter
        compileType();
        compileVarName();

        // Additional parameters
        while (tokenizer.symbol() == ',') {
            handleSymbol(',');
            compileType();
            compileVarName();
        }
    }

    /**
     * Compiles a subroutine's body.
     */
    public void compileSubroutineBody() throws IOException {
        handleSymbol('{');
        
        // Local variables
        while (isVarDec()) {
            compileVarDec();
        }
        
        compileStatements();
        handleSymbol('}');
    }

    /**
     * Compiles a var declaration.
     */
    public void compileVarDec() throws IOException {
        handleKeyword(KeywordType.VAR);
        compileType();
        compileVarName();
        
        while (tokenizer.symbol() == ',') {
            handleSymbol(',');
            compileVarName();
        }
        
        handleSymbol(';');
    }

    /**
     * Compiles a sequence of statements. Does not handle the enclosing curly brackets.
     */
    public void compileStatements() throws IOException {
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
    public void compileLet() throws IOException {
        handleKeyword(KeywordType.LET);
        handleIdentifier(); // var name
        
        // Array access
        if (tokenizer.symbol() == '[') {
            handleSymbol('[');
            compileExpression();
            handleSymbol(']');
        }
        
        handleSymbol('=');
        compileExpression();
        handleSymbol(';');
    }

    /**
     * Compiles an if statement, possibly with a trailing else clause.
     */
    public void compileIf() throws IOException {
        handleKeyword(KeywordType.IF);
        handleSymbol('(');
        compileExpression();
        handleSymbol(')');
        
        handleSymbol('{');
        compileStatements();
        handleSymbol('}');
        
        // Optional else clause
        if (tokenizer.hasMoreTokens() && tokenizer.tokenType() == TokenType.KEYWORD 
            && tokenizer.keyword() == KeywordType.ELSE) {
            handleKeyword(KeywordType.ELSE);
            handleSymbol('{');
            compileStatements();
            handleSymbol('}');
        }
    }

    /**
     * Compiles a while statement.
     */
    public void compileWhile() throws IOException {
        handleKeyword(KeywordType.WHILE);
        handleSymbol('(');
        compileExpression();
        handleSymbol(')');
        handleSymbol('{');
        compileStatements();
        handleSymbol('}');
    }

    /**
     * Compiles a do statement.
     */
    public void compileDo() throws IOException {
        handleKeyword(KeywordType.DO);
        compileSubroutineCall();
        handleSymbol(';');
    }

    /**
     * Compiles a return statement.
     */
    public void compileReturn() throws IOException {
        handleKeyword(KeywordType.RETURN);
        if (tokenizer.symbol() != ';') {
            compileExpression();
        }
        handleSymbol(';');
    }

    // Compiles an expression.
    public void compileExpression() throws IOException {
        compileTerm();
        
        while (isOperator(tokenizer.symbol())) {
            char op = tokenizer.symbol();
            handleSymbol(op);
            compileTerm();
        }
    }

    // Compiles a term.
    public void compileTerm() throws IOException {
        TokenType type = tokenizer.tokenType();
        
        switch (type) {
            case INT_CONST:
                handleIntegerConstant();
                break;
                
            case STRING_CONST:
                handleStringConstant();
                break;
                
            case KEYWORD:
                KeywordType keyword = tokenizer.keyword();
                if (keyword == KeywordType.TRUE || keyword == KeywordType.FALSE || 
                    keyword == KeywordType.NULL || keyword == KeywordType.THIS) {
                    handleKeyword(keyword);
                }
                break;
                
            case IDENTIFIER:
                String identifier = tokenizer.identifier();
                handleIdentifier();
                
                // Look ahead for '[', '(', or '.'
                if (tokenizer.hasMoreTokens()) {
                    char symbol = tokenizer.symbol();
                    if (symbol == '[') {
                        // Array access
                        handleSymbol('[');
                        compileExpression();
                        handleSymbol(']');
                    } else if (symbol == '(' || symbol == '.') {
                        // Subroutine call
                        compileSubroutineCall();
                    }
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
                }
                break;
        }
    }

    /**
     * Compiles a (possibly empty) comma-separated list of expressions.
     * Returns the number of expressions in the list.
     */
    public int compileExpressionList() throws IOException {
        int count = 0;
        
        if (tokenizer.symbol() == ')') {
            return count;
        }
        
        compileExpression();
        count++;
        
        while (tokenizer.symbol() == ',') {
            handleSymbol(',');
            compileExpression();
            count++;
        }
        
        return count;
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
        
        writeKeyword(currentKeywordType);
        tokenizer.advance();
    }

    private void handleSymbol(char expected) throws IOException {
        if (tokenizer.tokenType() != TokenType.SYMBOL || tokenizer.symbol() != expected) {
            throw new IllegalStateException("Expected symbol '" + expected + "', got " + tokenizer.tokenType());
        }
        writeSymbol(tokenizer.symbol());
        tokenizer.advance();
    }

    private void handleIdentifier() throws IOException {
        if (tokenizer.tokenType() != TokenType.IDENTIFIER) {
            throw new IllegalStateException("Expected identifier, got " + tokenizer.tokenType());
        }
        writeIdentifier(tokenizer.identifier());
        tokenizer.advance();
    }

    private void handleIntegerConstant() throws IOException {
        if (tokenizer.tokenType() != TokenType.INT_CONST) {
            throw new IllegalStateException("Expected integer constant, got " + tokenizer.tokenType());
        }
        writeIntegerConstant(tokenizer.intVal());
        tokenizer.advance();
    }

    private void handleStringConstant() throws IOException {
        if (tokenizer.tokenType() != TokenType.STRING_CONST) {
            throw new IllegalStateException("Expected string constant, got " + tokenizer.tokenType());
        }
        writeStringConstant(tokenizer.stringVal());
        tokenizer.advance();
    }

    private boolean isClassVarDec() throws IOException {
        return tokenizer.tokenType() == TokenType.KEYWORD && 
               (tokenizer.keyword() == KeywordType.STATIC || tokenizer.keyword() == KeywordType.FIELD);
    }

    private boolean isSubroutine() throws IOException {
        return tokenizer.tokenType() == TokenType.KEYWORD && 
               (tokenizer.keyword() == KeywordType.CONSTRUCTOR || 
                tokenizer.keyword() == KeywordType.FUNCTION || 
                tokenizer.keyword() == KeywordType.METHOD);
    }

    private boolean isVarDec() throws IOException {
        return tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyword() == KeywordType.VAR;
    }

    private boolean isOperator(char c) {
        return "+-*/&|<>=".indexOf(c) != -1;
    }

    private void compileType() throws IOException {
        if (tokenizer.tokenType() == TokenType.KEYWORD) {
            handleKeyword(KeywordType.INT, KeywordType.CHAR, KeywordType.BOOLEAN);
        } else {
            handleIdentifier(); // class/object name as type
        }
    }

    private void compileVarName() throws IOException {
        handleIdentifier();
    }

    private void compileSubroutineCall() throws IOException {
        if (tokenizer.symbol() == '.') {
            handleSymbol('.');
            handleIdentifier();
        }
        
        handleSymbol('(');
        compileExpressionList();
        handleSymbol(')');
    }

    private void writeKeyword(KeywordType keyword) {
        indent();
        writer.println("<keyword> " + keyword.toString().toLowerCase() + " </keyword>");
    }

    private void writeSymbol(char symbol) {
        indent();
        // Handle special XML characters
        String symbolStr = String.valueOf(symbol);
        switch (symbol) {
            case '<': symbolStr = "&lt;"; break;
            case '>': symbolStr = "&gt;"; break;
            case '&': symbolStr = "&amp;"; break;
        }
        writer.println("<symbol> " + symbolStr + " </symbol>");
    }

    private void writeIdentifier(String identifier) {
        indent();
        writer.println("<identifier> " + identifier + " </identifier>");
    }

    private void writeIntegerConstant(int value) {
        indent();
        writer.println("<integerConstant> " + value + " </integerConstant>");
    }

    private void writeStringConstant(String str) {
        indent();
        writer.println("<stringConstant> " + str + " </stringConstant>");
    }
    
    private void indent() {
        for (int i = 0; i < indentLevel; i++) {
            writer.print("   ");
        }
    }

    private void openExp(String name) {
        indent();
        writer.println("<" + name + ">");
        indentLevel++;
    }

    private void closeExp(String name) {
        indentLevel--;
        indent();
        writer.println("</" + name + ">");
    }

}
