import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    private Map<String, Symbol> classScope;
    private Map<String, Symbol> subroutineScope;
    private int staticIndex;
    private int fieldIndex;
    private int argIndex;
    private int varIndex;
    
    private static class Symbol {
        String name;
        String type;
        String kind;
        int scope;
        
        Symbol(String name, String type, String kind, int scope) {
            this.name = name;
            this.type = type;
            this.kind = kind;
            this.scope = scope;
        }
    }

    /**
     * Creates a new empty symbol table.
     */
    public SymbolTable() {
        classScope = new HashMap<>();
        subroutineScope = new HashMap<>();
        reset();
    }

    /**
     * Empties the symbol table, and resets the four indexes to 0.
     * Should be called when starting to compile a subroutine declaration.
     */
    public void reset() {
        subroutineScope.clear();
        staticIndex = 0;
        fieldIndex = 0;
        argIndex = 0;
        varIndex = 0;
    }

    /**
     * Defines a new identifier of the given name, type, and kind, and assigns it's index.
     */
    public void define(String name, String type, String kind) {
        Map<String, Symbol> scope = (kind.equals("STATIC") || kind.equals("FIELD")) ? classScope : subroutineScope;
        int index;
        
        switch (kind) {
            case "STATIC": index = staticIndex++; break;
            case "FIELD": index = fieldIndex++; break;
            case "ARG": index = argIndex++; break;
            case "VAR": index = varIndex++; break;
            default: throw new IllegalArgumentException("Invalid kind: " + kind);
        }
        
        scope.put(name, new Symbol(name, type, kind, index));
    }

    /**
     * Returns the number of variables of the given kind already defined in the current scope.
     */
    public int varCount(String kind) {
        switch (kind) {
            case "STATIC": return staticIndex;
            case "FIELD": return fieldIndex;
            case "ARG": return argIndex;
            case "VAR": return varIndex;
            default: throw new IllegalArgumentException("Invalid kind: " + kind);
        }
    }

    /**
     * Returns the kind of the named identifier.
     * If the identifier is not found, returns "NONE".
     */
    public String kindOf(String name) {
        // give priority to subroutine scope
        Symbol symbol = subroutineScope.get(name);
        if (symbol != null) return symbol.kind;
        
        symbol = classScope.get(name);
        if (symbol != null) return symbol.kind;
        
        return "NONE";
    }

    /**
     * Returns the type of the named variable.
     */
    public String typeOf(String name) {
        // give priority to subroutine scope
        Symbol symbol = subroutineScope.get(name);
        if (symbol != null) return symbol.type;
        
        symbol = classScope.get(name);
        if (symbol != null) return symbol.type;
        
        throw new IllegalArgumentException("Variable not found: " + name);
    }

    /**
     * Returns the index of the named variable.
     */
    public int indexOf(String name) {
        // give priority to subroutine scope
        Symbol symbol = subroutineScope.get(name);
        if (symbol != null) return symbol.scope;
        
        symbol = classScope.get(name);
        if (symbol != null) return symbol.scope;
        
        throw new IllegalArgumentException("Variable not found: " + name);
    }
}
