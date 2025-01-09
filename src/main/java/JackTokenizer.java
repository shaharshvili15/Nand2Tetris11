import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class JackTokenizer {
    private BufferedReader bReader;
    private String currentToken;
    private char currentChar;
    private StringBuilder currentTokenBuilder;

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
        int readChar = bReader.read();
        currentChar = (readChar == -1) ? '\0' : (char)readChar;
    }

    public boolean hasMoreTokens() {
        return currentChar != '\0';
    }

    public void advance() throws IOException {
        skipWhitespaceAndComments();
        if (!hasMoreTokens()) return;

        currentTokenBuilder.setLength(0); // Clear the builder

        if (isSymbol(currentChar)) {
            handleSymbol();
        } else if (currentChar == '"') {
            handleStringConstant();
        } else if (Character.isDigit(currentChar)) {
            handleIntegerConstant();
        } else if (isValidIdentifierStart(currentChar)) {
            handleIdentifierOrKeyword();
        }
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
        currentTokenBuilder.append(currentChar);
        readChar();
    }

    private void handleStringConstant() throws IOException {
        readChar(); // Skip opening quote
        while (hasMoreTokens() && currentChar != '"') {
            currentTokenBuilder.append(currentChar);
            readChar();
        }
        readChar(); // Skip closing quote
    }

    private void handleIntegerConstant() throws IOException {
        while (hasMoreTokens() && Character.isDigit(currentChar)) {
            currentTokenBuilder.append(currentChar);
            readChar();
        }
    }

    private void handleIdentifierOrKeyword() throws IOException {
        while (hasMoreTokens() && (Character.isLetterOrDigit(currentChar) || currentChar == '_')) {
            currentTokenBuilder.append(currentChar);
            readChar();
        }
    }

    public TokenType tokenType() {
        String token = currentTokenBuilder.toString();
        try {
            // Try to convert to enum - if it succeeds, it's a keyword
            KeywordType.valueOf(token.toUpperCase());
            return TokenType.KEYWORD;
        } catch (IllegalArgumentException e) {
            // Not a keyword, check other types
            if (token.length() == 1 && isSymbol(token.charAt(0))) return TokenType.SYMBOL;
            if (token.matches("\\d+")) return TokenType.INT_CONST;
            if (token.startsWith("\"") && token.endsWith("\"")) return TokenType.STRING_CONST;
            return TokenType.IDENTIFIER;
        }
    }

    public KeywordType keyword() {
        return KeywordType.valueOf(currentTokenBuilder.toString().toUpperCase());
    }

    public char symbol() {
        return currentTokenBuilder.charAt(0);
    }

    public int intVal() {
        return Integer.parseInt(currentTokenBuilder.toString());
    }

    public String stringVal() {
        return currentTokenBuilder.toString();
    }

    public String identifier() {
        return currentTokenBuilder.toString();
    }

    private boolean isSymbol(char c) {
        return "{}()[].,;+-*/&|<>=~".indexOf(c) != -1;
    }

    private boolean isValidIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    public void close() throws IOException {
        bReader.close();
    }
}
