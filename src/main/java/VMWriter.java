import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class VMWriter {
    private PrintWriter writer;

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
        writer.println("push " + segment.toLowerCase() + " " + index);
    }

    /**
     * Writes a VM pop command.
     */
    public void writePop(String segment, int index) {
        writer.println("pop " + segment.toLowerCase() + " " + index);
    }

    /**
     * Writes a VM arithmetic/logical command.
     */
    public void writeArithmetic(String command) {
        writer.println(command.toLowerCase());
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
        writer.println("goto " + label);
    }

    /**
     * Writes a VM if-goto command.
     */
    public void writeIf(String label) {
        writer.println("if-goto " + label);
    }

    /**
     * Writes a VM call command.
     */
    public void writeCall(String name, int nArgs) {
        writer.println("call " + name + " " + nArgs);
    }

    /**
     * Writes a VM function command.
     */
    public void writeFunction(String name, int nLocals) {
        writer.println("call " + name + " " + nLocals);
    }

    /**
     * Writes a VM return command.
     */
    public void writeReturn() {
        writer.println("return");
    }

    /**
     * Closes the output file.
     */
    public void close() {
        writer.close();
    }
}
