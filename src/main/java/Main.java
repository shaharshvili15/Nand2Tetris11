import java.io.File;

public class Main {
    public static void main(String[] args) {
        // if (args.length != 1) {
        //     System.out.println("Usage: java Main <file.jack or directory>");
        //     return;
        // }
        // Pass the argument to JackAnalyzer
        // JackCompiler.main(args);
        //JackCompiler.main(new String[]{"Tests/Seven"});
//        JackCompiler.main(new String[]{"Tests/myTest"});

        // to run all files:
        for (File file : new File("Tests").listFiles()) {
            System.out.println(file.getAbsolutePath());
            JackCompiler.main(new String[]{file.getAbsolutePath()});
        }
    }
}
