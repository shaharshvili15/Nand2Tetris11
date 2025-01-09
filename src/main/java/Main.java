public class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java Main <file.jack or directory>");
            return;
        }
        // Pass the argument to JackAnalyzer
        JackAnalyzer.main(args);
    }
}