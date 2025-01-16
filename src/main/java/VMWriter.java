import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class VMWriter {
    private PrintWriter writer;
    private static final String INDENT = "    ";  // 4 spaces for indentation

    /**
     * Creates a new output .vm file and prepares it for writing.
     */
    public VMWriter(String outputFile) throws IOException {
        writer = new PrintWriter(new FileWriter(outputFile));
    }

    /**
     * Writes a VM push command.
     */
    public void writePush(String segment, int index) {
        writer.println(INDENT + "push " + segment + " " + index);
    }

    /**
     * Writes a VM pop command.
     */
    public void writePop(String segment, int index) {
        writer.println(INDENT + "pop " + segment + " " + index);
    }

    /**
     * Writes a VM arithmetic/logical command.
     */
    public void writeArithmetic(String command) {
        writer.println(INDENT + command);
    }

    /**
     * Writes a VM label command.
     */
    public void writeLabel(String label) {
        writer.println("label " + label);
    }

    /**
     * Writes a VM goto command.
     */
    public void writeGoto(String label) {
        writer.println(INDENT + "goto " + label);
    }

    /**
     * Writes a VM if-goto command.
     */
    public void writeIf(String label) {
        writer.println(INDENT + "if-goto " + label);
    }

    /**
     * Writes a VM call command.
     */
    public void writeCall(String name, int nArgs) {
        writer.println(INDENT + "call " + name + " " + nArgs);
    }

    /**
     * Writes a VM function command.
     */
    public void writeFunction(String name, int nLocals) {
        writer.println("function " + name + " " + nLocals);
    }

    /**
     * Writes a VM return command.
     */
    public void writeReturn() {
        writer.println(INDENT + "return");
    }

    /**
     * Closes the output file.
     */
    public void close() {
        writer.close();
    }
}
