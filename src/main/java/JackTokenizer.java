import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class JackTokenizer {
    private BufferedReader bReader;
    private StringBuilder currentTokenBuilder;
    private char currentChar;
    private String currentToken;
    private TokenType currentTokenType;

    // Initialize the tokenizer with the input file
    public JackTokenizer(File inputFile) throws IOException {
        if (!inputFile.exists()) throw new IOException("File not found: " + inputFile);
        bReader = new BufferedReader(new FileReader(inputFile));
        currentTokenBuilder = new StringBuilder();
        readChar(); // Load first character
    }

    public JackTokenizer(String inputFile) throws IOException {
        this(new File(inputFile));
    }

    private void readChar() throws IOException {
        int charAsInt = bReader.read();
        currentChar = (charAsInt == -1) ? '\0' : (char)charAsInt; // '/0' as null for char
    }

    public boolean hasMoreTokens() {
        try {
            skipWhitespaceAndComments();
            return currentChar != '\0';
        } catch (IOException e) {
            return false;
        }
    }

    public void advance() throws IOException {
        if (!hasMoreTokens()) {
            throw new IllegalStateException("Called advance when no more tokens");
        }

        currentTokenBuilder = new StringBuilder();; // Reset the current token
        currentTokenType = null; // Reset the current token type

        if (isSymbol(currentChar)) {
            currentTokenType = TokenType.SYMBOL;
            handleSymbol();
        } else if (currentChar == '"') {
            currentTokenType = TokenType.STRING_CONST;
            handleStringConstant();
        } else if (Character.isDigit(currentChar)) {
            currentTokenType = TokenType.INT_CONST;
            handleIntegerConstant();
        } else if (isValidIdentifierStart(currentChar)) {
            handleIdentifierOrKeyword();
        }

        currentToken = currentTokenBuilder.toString();
    }

    private void skipWhitespaceAndComments() throws IOException {
        while (hasMoreTokens()) {
            // Skip whitespace
            while (hasMoreTokens() && Character.isWhitespace(currentChar)) {
                readChar();
            }

            // Skip single-line comments
            if (currentChar == '/' && peekNext() == '/') {
                while (hasMoreTokens() && currentChar != '\n') {
                    readChar();
                }
                continue;
            }

            // Skip multi-line comments
            if (currentChar == '/' && peekNext() == '*') {
                readChar(); // skip /
                readChar(); // skip *
                while (hasMoreTokens()) {
                    if (currentChar == '*' && peekNext() == '/') {
                        readChar(); // skip *
                        readChar(); // skip /
                        break;
                    }
                    readChar();
                }
                continue;
            }
            break;
        }
    }

    private int peekNext() throws IOException {
        bReader.mark(1);
        int next = bReader.read();
        bReader.reset();
        return next;
    }

    private void handleSymbol() throws IOException {
        // Build the token
        currentTokenBuilder.append(currentChar);
        readChar();
    }

    private void handleStringConstant() throws IOException {
        readChar(); // Skip opening quote
        // Build the token
        while (hasMoreTokens() && currentChar != '"') {
            currentTokenBuilder.append(currentChar);
            readChar();
        }
        readChar(); // Skip closing quote
    }

    private void handleIntegerConstant() throws IOException {
        // Build the token
        while (hasMoreTokens() && Character.isDigit(currentChar)) {
            currentTokenBuilder.append(currentChar);
            readChar();
        }
    }

    private void handleIdentifierOrKeyword() throws IOException {
        // Build the token
        while (hasMoreTokens() && (Character.isLetterOrDigit(currentChar) || currentChar == '_')) {
            currentTokenBuilder.append(currentChar);
            readChar();
        }
        
        // After building the token, check if it's a keyword
        String token = currentTokenBuilder.toString();
        try {
            KeywordType.valueOf(token.toUpperCase());
            currentTokenType = TokenType.KEYWORD;
        } catch (IllegalArgumentException e) {
            currentTokenType = TokenType.IDENTIFIER;
        }
    }


    public TokenType tokenType() {
        if (currentToken == null) {
            throw new IllegalStateException("No current token");
        }
        return (TokenType.valueOf(currentToken.toUpperCase()));
    }

    public KeywordType keyword() {
        if (tokenType() != TokenType.KEYWORD) {
            throw new IllegalStateException("Current token is not a keyword");
        }
        return KeywordType.valueOf(currentToken.toUpperCase());
    }

    public char symbol() {
        if (tokenType() != TokenType.SYMBOL) {
            throw new IllegalStateException("Current token is not a symbol");
        }
        return currentToken.charAt(0);
    }

    public int intVal() {
        if (tokenType() != TokenType.INT_CONST) {
            throw new IllegalStateException("Current token is not an integer constant");
        }
        return Integer.parseInt(currentToken);
    }

    public String stringVal() {
        if (tokenType() != TokenType.STRING_CONST) {
            throw new IllegalStateException("Current token is not a string constant");
        }
        return currentToken;
    }

    public String identifier() {
        if (tokenType() != TokenType.IDENTIFIER) {
            throw new IllegalStateException("Current token is not an identifier");
        }
        return currentToken;
    }

    private boolean isSymbol(char c) {
        return ("{}()[].,;+-*/&|<>=~".indexOf(c) != -1);
    }

    private boolean isValidIdentifierStart(char c) {
        return (Character.isLetter(c) || c == '_');
    }

    public void close() throws IOException {
        bReader.close();
    }
}
