import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

public class RefactorLog {
    public static void main(String[] args) throws Exception {
        Path src = Paths.get("src/main/java");
        List<Path> files = Files.walk(src)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .collect(Collectors.toList());

        for (Path p : files) {
            String content = new String(Files.readAllBytes(p));
            boolean modified = false;

            if (content.contains("System.out.println(")) {
                content = content.replaceAll("System\\.out\\.println\\(", "log.info(");
                modified = true;
            }
            if (content.contains("System.err.println(")) {
                content = content.replaceAll("System\\.err\\.println\\(", "log.error(");
                modified = true;
            }
            
            Matcher m = Pattern.compile("(\\w+)\\.printStackTrace\\(\\);").matcher(content);
            if (m.find()) {
                content = m.replaceAll("log.error(\"Exception occurred\", $1);");
                modified = true;
            }

            if (modified) {
                if (!content.contains("@Slf4j")) {
                    // find class declaration
                    content = content.replaceFirst("(?s)(public\\s+(?:final\\s+)?(?:abstract\\s+)?class)", "@Slf4j\n$1");
                    // add import if missing
                    if (!content.contains("import lombok.extern.slf4j.Slf4j;")) {
                        content = content.replaceFirst("package\\s+[^;]+;\n", "$0\nimport lombok.extern.slf4j.Slf4j;\n");
                    }
                }
                Files.write(p, content.getBytes());
                System.out.println("Modified " + p);
            }
        }
    }
}
