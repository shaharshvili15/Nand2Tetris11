import java.io.File;
import java.io.IOException;

public class JackCompiler {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java JackCompiler <input file/directory>");
            System.exit(1);
        }

        try {
            File input = new File(args[0]);
            if (!input.exists()) {
                throw new IOException("Input file/directory does not exist: " + input);
            }

            if (input.isFile()) {
                // Handle single file
                if (!input.getName().endsWith(".jack")) {
                    throw new IOException("Input file must have .jack extension");
                }
                compileFile(input);
            } else {
                File[] jackFiles = input.listFiles((directory, fileName) -> fileName.endsWith(".jack"));

                if (jackFiles == null || jackFiles.length == 0) {
                    throw new IOException("No .jack files found in directory: " + input);
                }
                for (File jackFile : jackFiles) {
                    compileFile(jackFile);
                }
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void compileFile(File inputFile) throws IOException {
        // Create output file with .vm suffix instead of .xml
        String outputPath = inputFile.getAbsolutePath();
        outputPath = outputPath.substring(0, outputPath.lastIndexOf(".")) + ".vm";
        File outputFile = new File(outputPath);

        // Create compilation engine and compile the class
        CompilationEngine engine = new CompilationEngine(inputFile, outputFile);
        engine.compileClass();
    }
}
